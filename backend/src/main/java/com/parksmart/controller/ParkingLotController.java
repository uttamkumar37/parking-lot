package com.parksmart.controller;

import com.parksmart.dto.response.ApiResponse;
import com.parksmart.dto.response.SlotAvailabilityResponse;
import com.parksmart.entity.ParkingLot;
import com.parksmart.service.ParkingLotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/parking")
@RequiredArgsConstructor
@Tag(name = "Parking Lots", description = "Parking lot discovery and slot availability")
public class ParkingLotController {

    private final ParkingLotService parkingLotService;

    @GetMapping("/lots")
    @Operation(summary = "List all active parking lots")
    public ResponseEntity<ApiResponse<List<ParkingLot>>> getAllLots() {
        return ResponseEntity.ok(
            ApiResponse.success("Lots retrieved", parkingLotService.getAllActiveLots())
        );
    }

    @GetMapping("/lots/{lotId}/availability")
    @Operation(summary = "Real-time slot availability for a lot")
    public ResponseEntity<ApiResponse<SlotAvailabilityResponse>> getAvailability(
        @PathVariable UUID lotId
    ) {
        return ResponseEntity.ok(
            ApiResponse.success("Availability retrieved", parkingLotService.getAvailability(lotId))
        );
    }
}
