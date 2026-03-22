package com.parksmart.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.Map;
import java.util.UUID;

@Data @Builder
public class SlotAvailabilityResponse {
    private UUID lotId;
    private String lotName;
    private String address;
    private int totalFloors;
    private Map<String, Long> availableByType;   // SlotType -> count
    private long totalAvailable;
    private long totalCapacity;
    private double occupancyPercent;
    private long cachedAtEpochMs;
}
