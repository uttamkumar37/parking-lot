package com.parksmart.service;

import com.parksmart.dto.request.ParkVehicleRequest;
import com.parksmart.dto.response.BookingResponse;
import com.parksmart.dto.response.BookingResponse.BillInfo;
import com.parksmart.entity.*;
import com.parksmart.entity.Booking.BookingStatus;
import com.parksmart.entity.ParkingSlot.SlotStatus;
import com.parksmart.entity.ParkingSlot.SlotType;
import com.parksmart.event.BookingEvent;
import com.parksmart.event.BookingEventPublisher;
import com.parksmart.exception.BadRequestException;
import com.parksmart.exception.NotFoundException;
import com.parksmart.exception.SlotNotAvailableException;
import com.parksmart.factory.PricingStrategyFactory;
import com.parksmart.factory.VehicleSlotFactory;
import com.parksmart.repository.*;
import com.parksmart.strategy.PricingStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private static final BigDecimal TAX_RATE = new BigDecimal("0.10"); // 10%

    private final BookingRepository bookingRepository;
    private final VehicleRepository vehicleRepository;
    private final ParkingSlotRepository slotRepository;
    private final ParkingLotRepository lotRepository;
    private final BillRepository billRepository;
    private final VehicleSlotFactory vehicleSlotFactory;
    private final PricingStrategyFactory pricingStrategyFactory;
    private final BookingEventPublisher eventPublisher;

    @Value("${parking.default-grace-period-minutes:15}")
    private int gracePeriodMinutes;

    /**
     * Park a vehicle: find/create vehicle, allocate slot, create booking.
     * Chain of Responsibility: try preferred slot type → fallback types.
     */
    @Transactional
    @CacheEvict(value = "slotAvailability", key = "#req.lotId")
    public BookingResponse parkVehicle(UUID userId, ParkVehicleRequest req) {
        // 1. Validate lot
        ParkingLot lot = lotRepository.findById(req.getLotId())
            .orElseThrow(() -> new NotFoundException("Parking lot not found: " + req.getLotId()));

        if (!lot.isActive()) {
            throw new BadRequestException("Parking lot is not currently active");
        }

        // 2. Get or create vehicle
        Vehicle vehicle = vehicleRepository.findByLicensePlate(req.getLicensePlate())
            .orElseGet(() -> {
                User user = new User();
                user.setId(userId);
                return vehicleRepository.save(Vehicle.builder()
                    .owner(user)
                    .licensePlate(req.getLicensePlate())
                    .vehicleType(req.getVehicleType())
                    .brand(req.getBrand())
                    .model(req.getModel())
                    .color(req.getColor())
                    .build());
            });

        // 3. Prevent double-parking
        if (bookingRepository.existsByVehicleIdAndStatus(vehicle.getId(), BookingStatus.ACTIVE)) {
            throw new BadRequestException("Vehicle " + req.getLicensePlate() + " is already parked");
        }

        // 4. Slot allocation — selected slot or Chain of Responsibility fallback
        ParkingSlot allocatedSlot = req.getSlotId() != null
            ? getRequestedSlot(req.getLotId(), req.getSlotId(), req.getVehicleType())
            : allocateSlot(req.getLotId(), req.getVehicleType());

        // 5. Mark slot OCCUPIED (atomic update)
        int updated = slotRepository.updateStatusIfCurrent(
            allocatedSlot.getId(),
            SlotStatus.AVAILABLE,
            SlotStatus.OCCUPIED
        );
        if (updated == 0) {
            throw new SlotNotAvailableException("Slot just became unavailable, please retry");
        }

        // 6. Create booking
        Booking booking = Booking.builder()
            .user(User.builder().id(userId).build())
            .vehicle(vehicle)
            .slot(allocatedSlot)
            .entryTime(LocalDateTime.now())
            .status(BookingStatus.ACTIVE)
            .build();

        booking = bookingRepository.save(booking);
        log.info("Vehicle {} parked at slot {} (booking {})",
            vehicle.getLicensePlate(), allocatedSlot.getSlotNumber(), booking.getId());

        // 7. Publish event (async via Kafka)
        eventPublisher.publish(BookingEvent.from(booking, BookingEvent.Type.BOOKING_CREATED).build());

        return toResponse(booking, null);
    }

    /**
     * Exit processing: calculate duration, generate bill, return bill + payment link placeholder.
     */
    @Transactional
    @CacheEvict(value = "slotAvailability", allEntries = true)
    public BookingResponse exitVehicle(UUID bookingId, UUID userId) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new NotFoundException("Booking not found: " + bookingId));

        if (!booking.getUser().getId().equals(userId)) {
            throw new BadRequestException("Booking does not belong to this user");
        }
        if (booking.getStatus() != BookingStatus.ACTIVE) {
            throw new BadRequestException("Booking is not active (status: " + booking.getStatus() + ")");
        }

        // 1. Calculate duration
        LocalDateTime exitTime = LocalDateTime.now();
        long minutes = ChronoUnit.MINUTES.between(booking.getEntryTime(), exitTime);
        minutes = Math.max(minutes, 1); // minimum 1 minute

        booking.setExitTime(exitTime);
        booking.setDurationMinutes(minutes);
        booking.setStatus(BookingStatus.PENDING_PAYMENT);

        // 2. Free the slot
        slotRepository.updateStatus(booking.getSlot().getId(), SlotStatus.AVAILABLE);

        // 3. Generate bill
        PricingStrategy strategy = pricingStrategyFactory.resolveForTime(booking.getEntryTime());
        BigDecimal base = strategy.calculateBase(booking);
        BigDecimal tax  = base.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = base.add(tax);

        Bill bill = Bill.builder()
            .booking(booking)
            .baseAmount(base)
            .taxAmount(tax)
            .totalAmount(total)
            .pricingStrategy(strategy.getStrategyName())
            .build();

        bill = billRepository.save(bill);
        booking = bookingRepository.save(booking);

        log.info("Exit processed for booking {} — bill: {}", bookingId, total);

        // 4. Publish event (notification + payment link creation handled async)
        eventPublisher.publish(BookingEvent.from(booking, BookingEvent.Type.BOOKING_COMPLETED)
            .totalAmount(bill.getTotalAmount())
            .build());

        return toResponse(booking, bill);
    }

    @Transactional(readOnly = true)
    public Page<BookingResponse> getBookingHistory(UUID userId, Pageable pageable) {
        return bookingRepository.findByUserId(userId, pageable)
            .map(b -> toResponse(b, b.getBill()));
    }

    @Transactional(readOnly = true)
    public BookingResponse getBooking(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new NotFoundException("Booking not found: " + bookingId));
        return toResponse(booking, booking.getBill());
    }

    @Transactional(readOnly = true)
    public BookingResponse getBooking(UUID bookingId, UUID requesterId, boolean isAdmin) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new NotFoundException("Booking not found: " + bookingId));
        verifyBookingAccess(booking, requesterId, isAdmin);
        return toResponse(booking, booking.getBill());
    }

    // ===================== Private Helpers =====================

    /**
     * Chain of Responsibility: try preferred slot type then fallbacks.
     */
    private ParkingSlot allocateSlot(UUID lotId, Vehicle.VehicleType vehicleType) {
        SlotType[] candidates = vehicleSlotFactory.getFallbackSlotTypes(vehicleType);
        for (SlotType slotType : candidates) {
            var slot = slotRepository.findFirstAvailableSlot(lotId, slotType);
            if (slot.isPresent()) return slot.get();
        }
        throw new SlotNotAvailableException(
            "No available slots for vehicle type " + vehicleType + " in this lot"
        );
    }

    private ParkingSlot getRequestedSlot(UUID lotId, UUID slotId, Vehicle.VehicleType vehicleType) {
        ParkingSlot slot = slotRepository.findById(slotId)
            .orElseThrow(() -> new NotFoundException("Parking slot not found: " + slotId));

        UUID slotLotId = slot.getFloor().getParkingLot().getId();
        if (!slotLotId.equals(lotId)) {
            throw new BadRequestException("Selected slot does not belong to this parking lot");
        }

        if (slot.getStatus() != SlotStatus.AVAILABLE) {
            throw new SlotNotAvailableException("Selected slot is not available");
        }

        boolean compatible = Arrays.asList(vehicleSlotFactory.getFallbackSlotTypes(vehicleType))
            .contains(slot.getSlotType());
        if (!compatible) {
            throw new BadRequestException(
                "Selected slot type " + slot.getSlotType() + " is not compatible with vehicle type " + vehicleType
            );
        }

        return slot;
    }

    private void verifyBookingAccess(Booking booking, UUID requesterId, boolean isAdmin) {
        if (isAdmin) return;
        if (booking.getUser() == null || !booking.getUser().getId().equals(requesterId)) {
            throw new AccessDeniedException("Booking does not belong to this user");
        }
    }

    private BookingResponse toResponse(Booking booking, Bill bill) {
        ParkingSlot slot = booking.getSlot();
        BillInfo billInfo = null;

        if (bill != null) {
            var payment = bill.getPayment();
            billInfo = BillInfo.builder()
                .billId(bill.getId())
                .baseAmount(bill.getBaseAmount())
                .taxAmount(bill.getTaxAmount())
                .totalAmount(bill.getTotalAmount())
                .pricingStrategy(bill.getPricingStrategy())
                .paymentLink(payment != null ? payment.getPaymentLink() : null)
                .paymentStatus(payment != null ? payment.getStatus().name() : "NOT_INITIATED")
                .build();
        }

        return BookingResponse.builder()
            .bookingId(booking.getId())
            .slotNumber(slot.getSlotNumber())
            .floorName(slot.getFloor().getFloorName())
            .lotName(slot.getFloor().getParkingLot().getName())
            .slotType(slot.getSlotType())
            .licensePlate(booking.getVehicle().getLicensePlate())
            .vehicleType(booking.getVehicle().getVehicleType())
            .entryTime(booking.getEntryTime())
            .exitTime(booking.getExitTime())
            .durationMinutes(booking.getDurationMinutes())
            .status(booking.getStatus())
            .bill(billInfo)
            .build();
    }
}
