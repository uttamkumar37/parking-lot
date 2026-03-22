package com.parksmart.controller;

import com.parksmart.dto.response.ApiResponse;
import com.parksmart.entity.Payment;
import com.parksmart.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Stripe payment sessions and webhook")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/{bookingId}/checkout")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create Stripe checkout session for a booking")
    public ResponseEntity<ApiResponse<String>> createCheckout(@PathVariable UUID bookingId) {
        String paymentLink = paymentService.createCheckoutSession(bookingId);
        return ResponseEntity.ok(ApiResponse.success("Payment link generated", paymentLink));
    }

    @GetMapping("/{bookingId}/status")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get payment status for a booking")
    public ResponseEntity<ApiResponse<Payment>> getStatus(@PathVariable UUID bookingId) {
        return ResponseEntity.ok(
            ApiResponse.success("Payment status retrieved", paymentService.getPaymentStatus(bookingId))
        );
    }

    /**
     * Stripe webhook — must remain public (no auth).
     * Verify using Stripe-Signature header.
     */
    @PostMapping("/webhook")
    @Operation(summary = "Stripe webhook endpoint (no auth)")
    public ResponseEntity<Void> handleWebhook(
        @RequestBody String payload,
        @RequestHeader("Stripe-Signature") String sigHeader
    ) {
        paymentService.handleWebhook(payload, sigHeader);
        return ResponseEntity.ok().build();
    }
}
