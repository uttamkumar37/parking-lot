package com.parksmart.dto.request;

import com.parksmart.entity.Vehicle.VehicleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ParkVehicleRequest {

    @NotBlank(message = "License plate is required")
    @Pattern(regexp = "^[A-Z0-9-]{2,10}$", message = "Invalid license plate format")
    private String licensePlate;

    @NotNull(message = "Vehicle type is required")
    private VehicleType vehicleType;

    private String brand;
    private String model;
    private String color;

    @NotNull(message = "Lot ID is required")
    private java.util.UUID lotId;
}
