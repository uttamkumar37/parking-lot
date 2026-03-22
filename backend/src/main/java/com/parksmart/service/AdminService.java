package com.parksmart.service;

import com.parksmart.dto.request.CreateLotRequest;
import com.parksmart.dto.response.AdminDashboardResponse;
import com.parksmart.dto.response.BookingResponse;
import com.parksmart.dto.response.RevenueReportResponse;
import com.parksmart.entity.ParkingFloor;
import com.parksmart.entity.ParkingLot;
import com.parksmart.entity.ParkingSlot;
import com.parksmart.entity.ParkingSlot.SlotStatus;
import com.parksmart.entity.ParkingSlot.SlotType;
import com.parksmart.entity.User;
import com.parksmart.exception.NotFoundException;
import com.parksmart.repository.BookingRepository;
import com.parksmart.repository.ParkingLotRepository;
import com.parksmart.repository.ParkingSlotRepository;
import com.parksmart.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final BookingRepository bookingRepository;
    private final ParkingLotRepository lotRepository;
    private final ParkingSlotRepository slotRepository;
    private final UserRepository userRepository;

    // ─── Dashboard ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboard() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime now = LocalDateTime.now();

        // Active bookings across all lots
        long activeBookings = bookingRepository.count() > 0
            ? bookingRepository.findAll().stream()
                .filter(b -> b.getStatus().name().equals("ACTIVE"))
                .count()
            : 0;

        // Today's bookings
        long totalToday = bookingRepository.findAll().stream()
            .filter(b -> b.getEntryTime() != null
                && b.getEntryTime().isAfter(todayStart)
                && b.getEntryTime().isBefore(now))
            .count();

        // Today's revenue
        BigDecimal revenueToday = bookingRepository.sumRevenueInPeriod(todayStart, now);

        // Lot stats
        List<ParkingLot> activeLots = lotRepository.findByActiveTrue();
        long totalSlots = slotRepository.count();
        long availableSlots = slotRepository.findAll().stream()
            .filter(s -> s.getStatus() == SlotStatus.AVAILABLE)
            .count();

        double occupancyRate = totalSlots > 0
            ? ((double)(totalSlots - availableSlots) / totalSlots) * 100.0
            : 0.0;

        // Slot availability by type
        Map<String, Long> byType = new LinkedHashMap<>();
        for (SlotType t : SlotType.values()) {
            byType.put(t.name(), 0L);
        }
        slotRepository.findAll().stream()
            .filter(s -> s.getStatus() == SlotStatus.AVAILABLE)
            .forEach(s -> byType.merge(s.getSlotType().name(), 1L, Long::sum));

        // Last 24-h occupancy history
        List<Object[]> rawHistory = bookingRepository.getHourlyOccupancyHistory(
            now.minusHours(24));
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:00");
        List<AdminDashboardResponse.HourlyDataPoint> history = rawHistory.stream()
            .map(row -> AdminDashboardResponse.HourlyDataPoint.builder()
                .hour(row[0].toString())
                .count(((Number) row[1]).longValue())
                .build())
            .toList();

        return AdminDashboardResponse.builder()
            .activeBookings(activeBookings)
            .totalBookingsToday(totalToday)
            .revenueToday(revenueToday != null ? revenueToday : BigDecimal.ZERO)
            .occupancyRate(Math.round(occupancyRate * 10.0) / 10.0)
            .totalActiveLots(activeLots.size())
            .totalSlots(totalSlots)
            .availableSlots(availableSlots)
            .slotAvailabilityByType(byType)
            .occupancyHistory(history)
            .build();
    }

    // ─── Bookings ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<BookingResponse> getAllBookings(Pageable pageable) {
        return bookingRepository.findAll(pageable)
            .map(b -> BookingResponse.builder()
                .bookingId(b.getId())
                .slotNumber(b.getSlot().getSlotNumber())
                .floorName(b.getSlot().getFloor().getFloorName())
                .lotName(b.getSlot().getFloor().getParkingLot().getName())
                .slotType(b.getSlot().getSlotType())
                .licensePlate(b.getVehicle().getLicensePlate())
                .vehicleType(b.getVehicle().getVehicleType())
                .entryTime(b.getEntryTime())
                .exitTime(b.getExitTime())
                .durationMinutes(b.getDurationMinutes())
                .status(b.getStatus())
                .build());
    }

    // ─── Revenue ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public RevenueReportResponse getRevenueSummary(LocalDate from, LocalDate to) {
        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt = to.plusDays(1).atStartOfDay();

        BigDecimal total = bookingRepository.sumRevenueInPeriod(fromDt, toDt);
        if (total == null) total = BigDecimal.ZERO;

        long completedCount = bookingRepository.findAll().stream()
            .filter(b -> b.getStatus().name().equals("COMPLETED")
                && b.getExitTime() != null
                && !b.getExitTime().isBefore(fromDt)
                && b.getExitTime().isBefore(toDt))
            .count();

        BigDecimal avg = completedCount > 0
            ? total.divide(BigDecimal.valueOf(completedCount), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        return RevenueReportResponse.builder()
            .from(from)
            .to(to)
            .totalRevenue(total)
            .completedBookings(completedCount)
            .averagePerBooking(avg)
            .build();
    }

    // ─── Lots ────────────────────────────────────────────────────────────────────

    @Transactional
    public ParkingLot createLot(CreateLotRequest req) {
        ParkingLot lot = ParkingLot.builder()
            .name(req.getName())
            .address(req.getAddress())
            .city(req.getCity())
            .zipCode(req.getZipCode())
            .totalFloors(req.getTotalFloors())
            .active(true)
            .build();

        // Build floors and slots from the optional floor spec
        if (req.getFloors() != null && !req.getFloors().isEmpty()) {
            List<ParkingFloor> floors = new ArrayList<>();
            int floorNumber = 1;
            for (CreateLotRequest.FloorRequest fr : req.getFloors()) {
                ParkingFloor floor = ParkingFloor.builder()
                    .parkingLot(lot)
                    .floorNumber(floorNumber++)
                    .floorName(fr.getFloorName())
                    .build();

                List<ParkingSlot> slots = new ArrayList<>();
                slots.addAll(buildSlots(floor, SlotType.SMALL,     fr.getSmallSlots(),     slots.size()));
                slots.addAll(buildSlots(floor, SlotType.MEDIUM,    fr.getMediumSlots(),    slots.size()));
                slots.addAll(buildSlots(floor, SlotType.LARGE,     fr.getLargeSlots(),     slots.size()));
                slots.addAll(buildSlots(floor, SlotType.EV,        fr.getEvSlots(),        slots.size()));
                slots.addAll(buildSlots(floor, SlotType.OVERSIZED, fr.getOversizedSlots(), slots.size()));
                floor.setSlots(slots);
                floors.add(floor);
            }
            lot.setFloors(floors);
        }

        ParkingLot saved = lotRepository.save(lot);
        log.info("Created parking lot '{}' with id {}", saved.getName(), saved.getId());
        return saved;
    }

    @Transactional
    public void toggleLotActive(UUID lotId) {
        ParkingLot lot = lotRepository.findById(lotId)
            .orElseThrow(() -> new NotFoundException("Lot not found: " + lotId));
        lot.setActive(!lot.isActive());
        lotRepository.save(lot);
        log.info("Lot {} active={}", lotId, lot.isActive());
    }

    // ─── Users ───────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    private List<ParkingSlot> buildSlots(ParkingFloor floor, SlotType type, int count, int startIndex) {
        List<ParkingSlot> slots = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            slots.add(ParkingSlot.builder()
                .floor(floor)
                .slotType(type)
                .status(SlotStatus.AVAILABLE)
                .slotNumber(type.name().charAt(0) + String.valueOf(startIndex + i + 1))
                .hasEvCharger(type == SlotType.EV)
                .build());
        }
        return slots;
    }
}
