package com.parksmart.controller;

import com.parksmart.dto.request.ParkVehicleRequest;
import com.parksmart.dto.response.ApiResponse;
import com.parksmart.dto.response.BookingResponse;
import com.parksmart.entity.User;
import com.parksmart.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Bookings", description = "Park, exit, and manage bookings")
public class BookingController {

    private final BookingService bookingService;

    @PostMapping("/park")
    @Operation(summary = "Park a vehicle — allocates slot and creates booking")
    public ResponseEntity<ApiResponse<BookingResponse>> parkVehicle(
        @AuthenticationPrincipal User user,
        @Valid @RequestBody ParkVehicleRequest request
    ) {
        BookingResponse response = bookingService.parkVehicle(user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Vehicle parked successfully", response));
    }

    @PostMapping("/{bookingId}/exit")
    @Operation(summary = "Exit parking — calculates bill and returns payment link")
    public ResponseEntity<ApiResponse<BookingResponse>> exitVehicle(
        @AuthenticationPrincipal User user,
        @PathVariable UUID bookingId
    ) {
        BookingResponse response = bookingService.exitVehicle(bookingId, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Exit processed. Bill generated.", response));
    }

    @GetMapping("/my")
    @Operation(summary = "My booking history (paginated)")
    public ResponseEntity<ApiResponse<Page<BookingResponse>>> myBookings(
        @AuthenticationPrincipal User user,
        @PageableDefault(size = 10, sort = "createdAt") Pageable pageable
    ) {
        Page<BookingResponse> page = bookingService.getBookingHistory(user.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success("Bookings retrieved", page));
    }

    @GetMapping("/{bookingId}")
    @Operation(summary = "Get a specific booking by ID")
    public ResponseEntity<ApiResponse<BookingResponse>> getBooking(
        @AuthenticationPrincipal User user,
        @PathVariable UUID bookingId
    ) {
        BookingResponse response = bookingService.getBooking(
            bookingId,
            user.getId(),
            user.getRole() == User.Role.ADMIN
        );
        return ResponseEntity.ok(ApiResponse.success("Booking retrieved", response));
    }
}
