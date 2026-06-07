package com.parksmart.service;

import com.parksmart.dto.response.SlotAvailabilityResponse;
import com.parksmart.dto.response.SlotMapResponse;
import com.parksmart.entity.ParkingLot;
import com.parksmart.entity.ParkingSlot;
import com.parksmart.entity.ParkingSlot.SlotStatus;
import com.parksmart.entity.ParkingSlot.SlotType;
import com.parksmart.exception.NotFoundException;
import com.parksmart.repository.ParkingLotRepository;
import com.parksmart.repository.ParkingSlotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParkingLotService {

    private final ParkingLotRepository lotRepository;
    private final ParkingSlotRepository slotRepository;

    @Transactional(readOnly = true)
    public List<ParkingLot> getAllActiveLots() {
        return lotRepository.findByActiveTrue();
    }

    /**
     * Slot availability — cached in Redis for 30 seconds.
     * Cache key = lot UUID; evicted on any booking park/exit.
     */
    @Cacheable(value = "slotAvailability", key = "#lotId")
    @Transactional(readOnly = true)
    public SlotAvailabilityResponse getAvailability(UUID lotId) {
        ParkingLot lot = lotRepository.findById(lotId)
            .orElseThrow(() -> new NotFoundException("Lot not found: " + lotId));

        List<Object[]> rows = slotRepository.countAvailableSlotsByTypeLot(lotId);

        Map<String, Long> available = new LinkedHashMap<>();
        long totalAvailable = 0;
        for (Object[] row : rows) {
            SlotType type = (SlotType) row[0];
            long count = (Long) row[1];
            available.put(type.name(), count);
            totalAvailable += count;
        }

        // All slot types with 0 if none available
        for (SlotType t : SlotType.values()) {
            available.putIfAbsent(t.name(), 0L);
        }

        long totalCapacity = lot.getFloors().stream()
            .mapToLong(f -> f.getSlots().size())
            .sum();

        double occupancy = totalCapacity > 0
            ? ((double)(totalCapacity - totalAvailable) / totalCapacity) * 100
            : 0;

        log.debug("Availability for lot {}: {}/{} available", lotId, totalAvailable, totalCapacity);

        return SlotAvailabilityResponse.builder()
            .lotId(lot.getId())
            .lotName(lot.getName())
            .address(lot.getAddress())
            .totalFloors(lot.getTotalFloors())
            .availableByType(available)
            .totalAvailable(totalAvailable)
            .totalCapacity(totalCapacity)
            .occupancyPercent(Math.round(occupancy * 10.0) / 10.0)
            .cachedAtEpochMs(System.currentTimeMillis())
            .build();
    }

    @Transactional(readOnly = true)
    public SlotMapResponse getSlotMap(UUID lotId) {
        ParkingLot lot = lotRepository.findById(lotId)
            .orElseThrow(() -> new NotFoundException("Lot not found: " + lotId));

        List<SlotMapResponse.FloorResponse> floors = lot.getFloors().stream()
            .sorted(Comparator.comparingInt(floor -> floor.getFloorNumber()))
            .map(floor -> {
                List<ParkingSlot> slots = floor.getSlots().stream()
                    .sorted(Comparator.comparing(ParkingSlot::getSlotNumber))
                    .toList();

                Map<String, List<SlotMapResponse.SlotResponse>> bySection = IntStream
                    .range(0, slots.size())
                    .mapToObj(index -> Map.entry(
                        sectionName(index),
                        toSlotResponse(slots.get(index), sectionSlotCode(index))
                    ))
                    .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        LinkedHashMap::new,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())
                    ));

                List<SlotMapResponse.SectionResponse> sections = bySection.entrySet().stream()
                    .map(entry -> SlotMapResponse.SectionResponse.builder()
                        .sectionName(entry.getKey())
                        .slots(entry.getValue())
                        .build())
                    .toList();

                return SlotMapResponse.FloorResponse.builder()
                    .floorId(floor.getId())
                    .floorNumber(floor.getFloorNumber())
                    .floorName(floor.getFloorName())
                    .totalSlots(slots.size())
                    .availableSlots(countStatus(slots, SlotStatus.AVAILABLE))
                    .occupiedSlots(countStatus(slots, SlotStatus.OCCUPIED))
                    .reservedSlots(countStatus(slots, SlotStatus.RESERVED))
                    .disabledSlots(countStatus(slots, SlotStatus.MAINTENANCE))
                    .sections(sections)
                    .build();
            })
            .toList();

        return SlotMapResponse.builder()
            .lotId(lot.getId())
            .lotName(lot.getName())
            .address(lot.getAddress())
            .city(lot.getCity())
            .totalFloors(lot.getTotalFloors())
            .active(lot.isActive())
            .floors(floors)
            .build();
    }

    private SlotMapResponse.SlotResponse toSlotResponse(ParkingSlot slot, String code) {
        return SlotMapResponse.SlotResponse.builder()
            .id(slot.getId())
            .code(code)
            .slotNumber(slot.getSlotNumber())
            .type(slot.getSlotType())
            .status(slot.getStatus())
            .evSupported(slot.isHasEvCharger())
            .handicapAccessible(slot.isHandicapAccessible())
            .hourlyRate(hourlyRate(slot.getSlotType()))
            .build();
    }

    private long countStatus(List<ParkingSlot> slots, SlotStatus status) {
        return slots.stream()
            .filter(slot -> slot.getStatus() == status)
            .count();
    }

    private String sectionName(int index) {
        return String.valueOf((char) ('A' + (index / 20)));
    }

    private String sectionSlotCode(int index) {
        return "%s%02d".formatted(sectionName(index), (index % 20) + 1);
    }

    private BigDecimal hourlyRate(SlotType slotType) {
        return switch (slotType) {
            case SMALL -> new BigDecimal("2.00");
            case MEDIUM -> new BigDecimal("4.00");
            case LARGE -> new BigDecimal("6.00");
            case EV -> new BigDecimal("5.00");
            case OVERSIZED -> new BigDecimal("10.00");
        };
    }
}
