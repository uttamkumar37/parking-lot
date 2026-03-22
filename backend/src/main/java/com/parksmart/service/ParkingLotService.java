package com.parksmart.service;

import com.parksmart.dto.response.SlotAvailabilityResponse;
import com.parksmart.entity.ParkingLot;
import com.parksmart.entity.ParkingSlot.SlotType;
import com.parksmart.exception.NotFoundException;
import com.parksmart.repository.ParkingLotRepository;
import com.parksmart.repository.ParkingSlotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

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
}
