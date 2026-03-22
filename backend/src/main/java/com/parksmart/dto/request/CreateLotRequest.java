package com.parksmart.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class CreateLotRequest {

    @NotBlank(message = "Lot name is required")
    @Size(max = 100)
    private String name;

    @NotBlank(message = "Address is required")
    @Size(max = 255)
    private String address;

    @Size(max = 100)
    private String city;

    @Size(max = 50)
    private String zipCode;

    @Min(value = 1, message = "At least 1 floor required")
    @Max(value = 20, message = "Maximum 20 floors")
    private int totalFloors;

    /** Optional: pre-define floors and slot counts when creating the lot. */
    @Valid
    private List<FloorRequest> floors;

    @Data
    public static class FloorRequest {

        @NotBlank
        @Size(max = 50)
        private String floorName;

        @Min(1) @Max(500)
        private int smallSlots;

        @Min(0) @Max(500)
        private int mediumSlots;

        @Min(0) @Max(500)
        private int largeSlots;

        @Min(0) @Max(100)
        private int evSlots;

        @Min(0) @Max(50)
        private int oversizedSlots;
    }
}
