package com.parksmart.dto.response;

import com.parksmart.entity.ParkingSlot.SlotStatus;
import com.parksmart.entity.ParkingSlot.SlotType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class SlotMapResponse {
    private UUID lotId;
    private String lotName;
    private String address;
    private String city;
    private int totalFloors;
    private boolean active;
    private List<FloorResponse> floors;

    @Data
    @Builder
    public static class FloorResponse {
        private UUID floorId;
        private int floorNumber;
        private String floorName;
        private long totalSlots;
        private long availableSlots;
        private long occupiedSlots;
        private long reservedSlots;
        private long disabledSlots;
        private List<SectionResponse> sections;
    }

    @Data
    @Builder
    public static class SectionResponse {
        private String sectionName;
        private List<SlotResponse> slots;
    }

    @Data
    @Builder
    public static class SlotResponse {
        private UUID id;
        private String code;
        private String slotNumber;
        private SlotType type;
        private SlotStatus status;
        private boolean evSupported;
        private boolean handicapAccessible;
        private BigDecimal hourlyRate;
    }
}
