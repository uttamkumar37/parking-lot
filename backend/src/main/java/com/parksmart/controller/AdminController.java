package com.parksmart.controller;

import com.parksmart.dto.request.CreateLotRequest;
import com.parksmart.dto.response.AdminDashboardResponse;
import com.parksmart.dto.response.ApiResponse;
import com.parksmart.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin", description = "Admin-only management endpoints")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/dashboard")
    @Operation(summary = "Dashboard metrics: active bookings, revenue, occupancy")
    public ResponseEntity<ApiResponse<AdminDashboardResponse>> getDashboard() {
        return ResponseEntity.ok(
            ApiResponse.success("Dashboard data retrieved", adminService.getDashboard())
        );
    }

    @GetMapping("/bookings")
    @Operation(summary = "All bookings with pagination and filters")
    public ResponseEntity<ApiResponse<?>> getAllBookings(
        @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        return ResponseEntity.ok(
            ApiResponse.success("Bookings retrieved", adminService.getAllBookings(pageable))
        );
    }

    @GetMapping("/revenue")
    @Operation(summary = "Revenue report for a date range")
    public ResponseEntity<ApiResponse<?>> getRevenue(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(
            ApiResponse.success("Revenue data retrieved", adminService.getRevenueSummary(from, to))
        );
    }

    @PostMapping("/lots")
    @Operation(summary = "Create a new parking lot")
    public ResponseEntity<ApiResponse<?>> createLot(@Valid @RequestBody CreateLotRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Parking lot created", adminService.createLot(request)));
    }

    @PatchMapping("/lots/{lotId}/toggle")
    @Operation(summary = "Activate or deactivate a parking lot")
    public ResponseEntity<ApiResponse<Void>> toggleLot(@PathVariable UUID lotId) {
        adminService.toggleLotActive(lotId);
        return ResponseEntity.ok(ApiResponse.success("Lot status toggled", null));
    }

    @GetMapping("/users")
    @Operation(summary = "List all users")
    public ResponseEntity<ApiResponse<?>> getAllUsers(
        @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(
            ApiResponse.success("Users retrieved", adminService.getAllUsers(pageable))
        );
    }
}
