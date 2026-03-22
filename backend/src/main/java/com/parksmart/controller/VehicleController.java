package com.parksmart.controller;

import com.parksmart.dto.response.ApiResponse;
import com.parksmart.entity.User;
import com.parksmart.entity.Vehicle;
import com.parksmart.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/vehicles")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Vehicles", description = "Manage registered vehicles")
public class VehicleController {

    private final VehicleService vehicleService;

    @GetMapping
    @Operation(summary = "List my registered vehicles")
    public ResponseEntity<ApiResponse<List<Vehicle>>> myVehicles(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(
            ApiResponse.success("Vehicles retrieved", vehicleService.getVehiclesByUser(user.getId()))
        );
    }
}
