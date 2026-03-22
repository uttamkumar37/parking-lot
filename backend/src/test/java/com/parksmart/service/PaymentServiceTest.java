package com.parksmart.service;

import com.parksmart.entity.*;
import com.parksmart.entity.Booking.BookingStatus;
import com.parksmart.entity.Payment.PaymentStatus;
import com.parksmart.event.BookingEvent;
import com.parksmart.event.BookingEventPublisher;
import com.parksmart.exception.BadRequestException;
import com.parksmart.exception.NotFoundException;
import com.parksmart.repository.BillRepository;
import com.parksmart.repository.BookingRepository;
import com.parksmart.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock PaymentRepository paymentRepository;
    @Mock BillRepository billRepository;
    @Mock BookingRepository bookingRepository;
    @Mock BookingEventPublisher eventPublisher;

    @InjectMocks PaymentService paymentService;

    private UUID bookingId;
    private Booking booking;
    private Bill bill;
    private Payment existingPayment;

    @BeforeEach
    void setUp() {
        // Inject @Value fields using ReflectionTestUtils
        ReflectionTestUtils.setField(paymentService, "stripeSecretKey", "sk_test_placeholder");
        ReflectionTestUtils.setField(paymentService, "webhookSecret",   "whsec_placeholder");
        ReflectionTestUtils.setField(paymentService, "successUrl",      "http://localhost:3000/payment/success");
        ReflectionTestUtils.setField(paymentService, "cancelUrl",       "http://localhost:3000/payment/cancel");

        bookingId = UUID.randomUUID();
        UUID billId = UUID.randomUUID();

        ParkingFloor floor = ParkingFloor.builder()
            .id(UUID.randomUUID()).floorNumber(1).floorName("G")
            .parkingLot(ParkingLot.builder().id(UUID.randomUUID()).name("Test Lot").build())
            .build();

        ParkingSlot slot = ParkingSlot.builder()
            .id(UUID.randomUUID()).slotNumber("M-01")
            .slotType(ParkingSlot.SlotType.MEDIUM).floor(floor)
            .build();

        booking = Booking.builder()
            .id(bookingId)
            .user(User.builder().id(UUID.randomUUID()).email("user@test.com").build())
            .vehicle(Vehicle.builder().id(UUID.randomUUID()).licensePlate("TST1").build())
            .slot(slot)
            .status(BookingStatus.PENDING_PAYMENT)
            .durationMinutes(90L)
            .build();

        bill = Bill.builder()
            .id(billId)
            .booking(booking)
            .baseAmount(new BigDecimal("8.00"))
            .taxAmount(new BigDecimal("0.80"))
            .totalAmount(new BigDecimal("8.80"))
            .build();

        booking = Booking.builder()
            .id(bookingId)
            .user(booking.getUser())
            .vehicle(booking.getVehicle())
            .slot(slot)
            .status(BookingStatus.PENDING_PAYMENT)
            .durationMinutes(90L)
            .bill(bill)
            .build();

        existingPayment = Payment.builder()
            .id(UUID.randomUUID())
            .bill(bill)
            .status(PaymentStatus.PENDING)
            .paymentLink("https://checkout.stripe.com/pay/existing")
            .stripeSessionId("cs_test_existing")
            .amount(new BigDecimal("8.80"))
            .build();
    }

    // ── createCheckoutSession ─────────────────────────────────────────────────

    @Test
    void createCheckoutSession_withExistingLink_returnsExistingLink() {
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBillId(bill.getId()))
            .thenReturn(Optional.of(existingPayment));

        String link = paymentService.createCheckoutSession(bookingId);

        assertThat(link).isEqualTo("https://checkout.stripe.com/pay/existing");
        // Should NOT create a new Stripe session
        verifyNoMoreInteractions(billRepository);
    }

    @Test
    void createCheckoutSession_withUnknownBooking_throwsNotFoundException() {
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.createCheckoutSession(bookingId))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void createCheckoutSession_withNonPendingBooking_throwsBadRequest() {
        booking.setStatus(BookingStatus.COMPLETED);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> paymentService.createCheckoutSession(bookingId))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("awaiting payment");
    }

    // ── getPaymentStatus ──────────────────────────────────────────────────────

    @Test
    void getPaymentStatus_withExistingPayment_returnsPayment() {
        when(paymentRepository.findByBillBookingId(bookingId))
            .thenReturn(Optional.of(existingPayment));

        Payment result = paymentService.getPaymentStatus(bookingId);

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(result.getStripeSessionId()).isEqualTo("cs_test_existing");
    }

    @Test
    void getPaymentStatus_withNoPayment_throwsNotFoundException() {
        when(paymentRepository.findByBillBookingId(bookingId))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getPaymentStatus(bookingId))
            .isInstanceOf(NotFoundException.class);
    }
}
