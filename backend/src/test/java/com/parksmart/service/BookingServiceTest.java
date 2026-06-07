package com.parksmart.service;

import com.parksmart.dto.request.ParkVehicleRequest;
import com.parksmart.dto.response.BookingResponse;
import com.parksmart.entity.*;
import com.parksmart.entity.Booking.BookingStatus;
import com.parksmart.entity.ParkingSlot.SlotStatus;
import com.parksmart.entity.ParkingSlot.SlotType;
import com.parksmart.entity.Vehicle.VehicleType;
import com.parksmart.event.BookingEventPublisher;
import com.parksmart.exception.BadRequestException;
import com.parksmart.exception.NotFoundException;
import com.parksmart.exception.SlotNotAvailableException;
import com.parksmart.factory.PricingStrategyFactory;
import com.parksmart.factory.VehicleSlotFactory;
import com.parksmart.repository.*;
import com.parksmart.strategy.HourlyPricingStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock BookingRepository bookingRepository;
    @Mock VehicleRepository vehicleRepository;
    @Mock ParkingSlotRepository slotRepository;
    @Mock ParkingLotRepository lotRepository;
    @Mock BillRepository billRepository;
    @Mock VehicleSlotFactory vehicleSlotFactory;
    @Mock PricingStrategyFactory pricingStrategyFactory;
    @Mock BookingEventPublisher eventPublisher;

    @InjectMocks BookingService bookingService;

    private UUID userId;
    private UUID lotId;
    private ParkingLot parkingLot;
    private ParkingFloor floor;
    private ParkingSlot slot;
    private Vehicle vehicle;
    private Booking activeBooking;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(bookingService, "gracePeriodMinutes", 15);

        userId = UUID.randomUUID();
        lotId  = UUID.randomUUID();

        parkingLot = ParkingLot.builder()
            .id(lotId).name("Test Lot").address("1 Main St").active(true).totalFloors(1)
            .build();

        floor = ParkingFloor.builder()
            .id(UUID.randomUUID()).parkingLot(parkingLot).floorNumber(1).floorName("Ground")
            .build();

        slot = ParkingSlot.builder()
            .id(UUID.randomUUID()).floor(floor).slotNumber("M-01")
            .slotType(SlotType.MEDIUM).status(SlotStatus.AVAILABLE)
            .build();

        vehicle = Vehicle.builder()
            .id(UUID.randomUUID()).licensePlate("ABC123").vehicleType(VehicleType.CAR)
            .build();

        activeBooking = Booking.builder()
            .id(UUID.randomUUID())
            .user(User.builder().id(userId).build())
            .vehicle(vehicle)
            .slot(slot)
            .entryTime(LocalDateTime.now().minusHours(2))
            .status(BookingStatus.ACTIVE)
            .build();
    }

    // ── parkVehicle ───────────────────────────────────────────────────────────

    @Test
    void parkVehicle_withValidRequest_createsBooking() {
        ParkVehicleRequest req = new ParkVehicleRequest();
        req.setLotId(lotId);
        req.setLicensePlate("XYZ999");
        req.setVehicleType(VehicleType.CAR);

        Vehicle newVehicle = Vehicle.builder()
            .id(UUID.randomUUID()).licensePlate("XYZ999").vehicleType(VehicleType.CAR)
            .build();
        Booking savedBooking = Booking.builder()
            .id(UUID.randomUUID()).user(User.builder().id(userId).build())
            .vehicle(newVehicle).slot(slot)
            .entryTime(LocalDateTime.now()).status(BookingStatus.ACTIVE)
            .build();

        when(lotRepository.findById(lotId)).thenReturn(Optional.of(parkingLot));
        when(vehicleRepository.findByLicensePlate("XYZ999")).thenReturn(Optional.empty());
        when(vehicleRepository.save(any())).thenReturn(newVehicle);
        when(bookingRepository.existsByVehicleIdAndStatus(any(), eq(BookingStatus.ACTIVE)))
            .thenReturn(false);
        when(vehicleSlotFactory.getFallbackSlotTypes(VehicleType.CAR))
            .thenReturn(new SlotType[]{SlotType.MEDIUM});
        when(slotRepository.findFirstAvailableSlot(lotId, SlotType.MEDIUM))
            .thenReturn(Optional.of(slot));
        when(slotRepository.updateStatusIfCurrent(slot.getId(), SlotStatus.AVAILABLE, SlotStatus.OCCUPIED))
            .thenReturn(1);
        when(bookingRepository.save(any())).thenReturn(savedBooking);
        doNothing().when(eventPublisher).publish(any());

        BookingResponse response = bookingService.parkVehicle(userId, req);

        assertThat(response.getStatus()).isEqualTo(BookingStatus.ACTIVE);
        assertThat(response.getLicensePlate()).isEqualTo("XYZ999");
        verify(slotRepository).updateStatusIfCurrent(slot.getId(), SlotStatus.AVAILABLE, SlotStatus.OCCUPIED);
    }

    @Test
    void parkVehicle_withSelectedSlot_usesRequestedSlot() {
        ParkVehicleRequest req = new ParkVehicleRequest();
        req.setLotId(lotId);
        req.setSlotId(slot.getId());
        req.setLicensePlate("SEL001");
        req.setVehicleType(VehicleType.CAR);

        Vehicle newVehicle = Vehicle.builder()
            .id(UUID.randomUUID()).licensePlate("SEL001").vehicleType(VehicleType.CAR)
            .build();
        Booking savedBooking = Booking.builder()
            .id(UUID.randomUUID()).user(User.builder().id(userId).build())
            .vehicle(newVehicle).slot(slot)
            .entryTime(LocalDateTime.now()).status(BookingStatus.ACTIVE)
            .build();

        when(lotRepository.findById(lotId)).thenReturn(Optional.of(parkingLot));
        when(vehicleRepository.findByLicensePlate("SEL001")).thenReturn(Optional.empty());
        when(vehicleRepository.save(any())).thenReturn(newVehicle);
        when(bookingRepository.existsByVehicleIdAndStatus(any(), eq(BookingStatus.ACTIVE)))
            .thenReturn(false);
        when(slotRepository.findById(slot.getId())).thenReturn(Optional.of(slot));
        when(vehicleSlotFactory.getFallbackSlotTypes(VehicleType.CAR))
            .thenReturn(new SlotType[]{SlotType.MEDIUM, SlotType.LARGE});
        when(slotRepository.updateStatusIfCurrent(slot.getId(), SlotStatus.AVAILABLE, SlotStatus.OCCUPIED))
            .thenReturn(1);
        when(bookingRepository.save(any())).thenReturn(savedBooking);
        doNothing().when(eventPublisher).publish(any());

        BookingResponse response = bookingService.parkVehicle(userId, req);

        assertThat(response.getSlotNumber()).isEqualTo("M-01");
        verify(slotRepository, never()).findFirstAvailableSlot(any(), any());
        verify(slotRepository).updateStatusIfCurrent(slot.getId(), SlotStatus.AVAILABLE, SlotStatus.OCCUPIED);
    }

    @Test
    void parkVehicle_withIncompatibleSelectedSlot_throwsBadRequest() {
        ParkVehicleRequest req = new ParkVehicleRequest();
        req.setLotId(lotId);
        req.setSlotId(slot.getId());
        req.setLicensePlate("BUS001");
        req.setVehicleType(VehicleType.BUS);

        when(lotRepository.findById(lotId)).thenReturn(Optional.of(parkingLot));
        when(vehicleRepository.findByLicensePlate("BUS001")).thenReturn(Optional.empty());
        when(vehicleRepository.save(any())).thenReturn(vehicle);
        when(bookingRepository.existsByVehicleIdAndStatus(any(), eq(BookingStatus.ACTIVE)))
            .thenReturn(false);
        when(slotRepository.findById(slot.getId())).thenReturn(Optional.of(slot));
        when(vehicleSlotFactory.getFallbackSlotTypes(VehicleType.BUS))
            .thenReturn(new SlotType[]{SlotType.OVERSIZED});

        assertThatThrownBy(() -> bookingService.parkVehicle(userId, req))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("not compatible");

        verify(slotRepository, never()).updateStatusIfCurrent(any(), any(), any());
    }

    @Test
    void parkVehicle_withInactiveLot_throwsBadRequest() {
        parkingLot.setActive(false);
        when(lotRepository.findById(lotId)).thenReturn(Optional.of(parkingLot));

        ParkVehicleRequest req = new ParkVehicleRequest();
        req.setLotId(lotId);
        req.setLicensePlate("T-001");
        req.setVehicleType(VehicleType.CAR);

        assertThatThrownBy(() -> bookingService.parkVehicle(userId, req))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("not currently active");
    }

    @Test
    void parkVehicle_withAlreadyParkedVehicle_throwsBadRequest() {
        ParkVehicleRequest req = new ParkVehicleRequest();
        req.setLotId(lotId);
        req.setLicensePlate("ABC123");
        req.setVehicleType(VehicleType.CAR);

        when(lotRepository.findById(lotId)).thenReturn(Optional.of(parkingLot));
        when(vehicleRepository.findByLicensePlate("ABC123")).thenReturn(Optional.of(vehicle));
        when(bookingRepository.existsByVehicleIdAndStatus(vehicle.getId(), BookingStatus.ACTIVE))
            .thenReturn(true);

        assertThatThrownBy(() -> bookingService.parkVehicle(userId, req))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("already parked");
    }

    @Test
    void parkVehicle_withNoAvailableSlots_throwsSlotNotAvailable() {
        ParkVehicleRequest req = new ParkVehicleRequest();
        req.setLotId(lotId);
        req.setLicensePlate("NEW1");
        req.setVehicleType(VehicleType.CAR);

        when(lotRepository.findById(lotId)).thenReturn(Optional.of(parkingLot));
        when(vehicleRepository.findByLicensePlate("NEW1")).thenReturn(Optional.empty());
        when(vehicleRepository.save(any())).thenReturn(vehicle);
        when(bookingRepository.existsByVehicleIdAndStatus(any(), any())).thenReturn(false);
        when(vehicleSlotFactory.getFallbackSlotTypes(VehicleType.CAR))
            .thenReturn(new SlotType[]{SlotType.MEDIUM, SlotType.LARGE});
        when(slotRepository.findFirstAvailableSlot(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.parkVehicle(userId, req))
            .isInstanceOf(SlotNotAvailableException.class);
    }

    @Test
    void parkVehicle_whenTwoUsersRaceForSameSlot_onlyOneSucceeds() throws Exception {
        ParkVehicleRequest req1 = new ParkVehicleRequest();
        req1.setLotId(lotId);
        req1.setLicensePlate("CAR001");
        req1.setVehicleType(VehicleType.CAR);

        ParkVehicleRequest req2 = new ParkVehicleRequest();
        req2.setLotId(lotId);
        req2.setLicensePlate("CAR002");
        req2.setVehicleType(VehicleType.CAR);

        Vehicle vehicle1 = Vehicle.builder()
            .id(UUID.randomUUID()).licensePlate("CAR001").vehicleType(VehicleType.CAR)
            .build();
        Vehicle vehicle2 = Vehicle.builder()
            .id(UUID.randomUUID()).licensePlate("CAR002").vehicleType(VehicleType.CAR)
            .build();

        when(lotRepository.findById(lotId)).thenReturn(Optional.of(parkingLot));
        when(vehicleRepository.findByLicensePlate("CAR001")).thenReturn(Optional.empty());
        when(vehicleRepository.findByLicensePlate("CAR002")).thenReturn(Optional.empty());
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> {
            Vehicle vehicleToSave = invocation.getArgument(0);
            return "CAR001".equals(vehicleToSave.getLicensePlate()) ? vehicle1 : vehicle2;
        });
        when(bookingRepository.existsByVehicleIdAndStatus(any(), eq(BookingStatus.ACTIVE)))
            .thenReturn(false);
        when(vehicleSlotFactory.getFallbackSlotTypes(VehicleType.CAR))
            .thenReturn(new SlotType[]{SlotType.MEDIUM});
        when(slotRepository.findFirstAvailableSlot(lotId, SlotType.MEDIUM))
            .thenReturn(Optional.of(slot));

        AtomicInteger successfulOccupations = new AtomicInteger();
        when(slotRepository.updateStatusIfCurrent(slot.getId(), SlotStatus.AVAILABLE, SlotStatus.OCCUPIED))
            .thenAnswer(invocation -> successfulOccupations.compareAndSet(0, 1) ? 1 : 0);
        when(bookingRepository.save(any())).thenAnswer(invocation -> {
            Booking booking = invocation.getArgument(0);
            booking.setId(UUID.randomUUID());
            return booking;
        });
        doNothing().when(eventPublisher).publish(any());

        var executor = Executors.newFixedThreadPool(2);
        try {
            List<Callable<Object>> attempts = List.of(
                () -> bookingService.parkVehicle(userId, req1),
                () -> bookingService.parkVehicle(UUID.randomUUID(), req2)
            );

            var results = executor.invokeAll(attempts);

            long successes = results.stream().filter(future -> {
                try {
                    future.get();
                    return true;
                } catch (Exception ignored) {
                    return false;
                }
            }).count();
            long failures = results.stream().filter(future -> {
                try {
                    future.get();
                    return false;
                } catch (Exception ex) {
                    return ex.getCause() instanceof SlotNotAvailableException;
                }
            }).count();

            assertThat(successes).isEqualTo(1);
            assertThat(failures).isEqualTo(1);
            verify(bookingRepository, times(1)).save(any(Booking.class));
        } finally {
            executor.shutdown();
            assertThat(executor.awaitTermination(1, TimeUnit.SECONDS)).isTrue();
        }
    }

    // ── exitVehicle ───────────────────────────────────────────────────────────

    @Test
    void exitVehicle_withActiveBooking_generatesBill() {
        HourlyPricingStrategy strategy = new HourlyPricingStrategy();
        Bill savedBill = Bill.builder()
            .id(UUID.randomUUID())
            .booking(activeBooking)
            .baseAmount(new BigDecimal("20.00"))
            .taxAmount(new BigDecimal("2.00"))
            .totalAmount(new BigDecimal("22.00"))
            .pricingStrategy("HOURLY")
            .build();

        when(bookingRepository.findById(activeBooking.getId()))
            .thenReturn(Optional.of(activeBooking));
        when(slotRepository.updateStatus(any(), eq(SlotStatus.AVAILABLE))).thenReturn(1);
        when(pricingStrategyFactory.resolveForTime(any())).thenReturn(strategy);
        when(billRepository.save(any())).thenReturn(savedBill);
        when(bookingRepository.save(any())).thenReturn(activeBooking);
        doNothing().when(eventPublisher).publish(any());

        BookingResponse response = bookingService.exitVehicle(activeBooking.getId(), userId);

        assertThat(response).isNotNull();
        verify(slotRepository).updateStatus(slot.getId(), SlotStatus.AVAILABLE);
        verify(billRepository).save(any(Bill.class));
        verify(eventPublisher).publish(any());
    }

    @Test
    void exitVehicle_withWrongUser_throwsBadRequest() {
        when(bookingRepository.findById(activeBooking.getId()))
            .thenReturn(Optional.of(activeBooking));

        UUID differentUser = UUID.randomUUID();
        assertThatThrownBy(() -> bookingService.exitVehicle(activeBooking.getId(), differentUser))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("does not belong");
    }

    @Test
    void exitVehicle_withNonActiveBooking_throwsBadRequest() {
        activeBooking.setStatus(BookingStatus.COMPLETED);
        when(bookingRepository.findById(activeBooking.getId()))
            .thenReturn(Optional.of(activeBooking));

        assertThatThrownBy(() -> bookingService.exitVehicle(activeBooking.getId(), userId))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("not active");
    }

    @Test
    void exitVehicle_withUnknownBooking_throwsNotFoundException() {
        UUID unknownId = UUID.randomUUID();
        when(bookingRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.exitVehicle(unknownId, userId))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getBooking_withDifferentUser_throwsAccessDenied() {
        when(bookingRepository.findById(activeBooking.getId()))
            .thenReturn(Optional.of(activeBooking));

        assertThatThrownBy(() ->
            bookingService.getBooking(activeBooking.getId(), UUID.randomUUID(), false)
        ).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }
}
