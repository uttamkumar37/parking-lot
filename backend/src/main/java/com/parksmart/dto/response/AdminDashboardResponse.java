package com.parksmart.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class AdminDashboardResponse {

    // ---- Live metrics ----
    private long activeBookings;
    private long totalBookingsToday;
    private BigDecimal revenueToday;
    private double occupancyRate;          // percentage 0-100

    // ---- Lot overview ----
    private long totalActiveLots;
    private long totalSlots;
    private long availableSlots;

    // ---- Slot type breakdown: type → available count ----
    private Map<String, Long> slotAvailabilityByType;

    // ---- Recent hourly occupancy (last 24 h) ----
    private List<HourlyDataPoint> occupancyHistory;

    @Data
    @Builder
    public static class HourlyDataPoint {
        private String hour;   // ISO-8601 truncated to hour
        private long count;
    }
}
