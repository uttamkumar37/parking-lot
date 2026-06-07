package com.parksmart.service;

import com.parksmart.entity.Bill;
import com.parksmart.entity.Booking;
import com.parksmart.entity.Booking.BookingStatus;
import com.parksmart.entity.Payment;
import com.parksmart.entity.Payment.PaymentStatus;
import com.parksmart.event.BookingEvent;
import com.parksmart.event.BookingEventPublisher;
import com.parksmart.exception.BadRequestException;
import com.parksmart.exception.NotFoundException;
import com.parksmart.repository.BillRepository;
import com.parksmart.repository.BookingRepository;
import com.parksmart.repository.PaymentRepository;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BillRepository billRepository;
    private final BookingRepository bookingRepository;
    private final BookingEventPublisher eventPublisher;

    @Value("${stripe.secret-key}")
    private String stripeSecretKey;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    @Value("${stripe.success-url}")
    private String successUrl;

    @Value("${stripe.cancel-url}")
    private String cancelUrl;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    /**
     * Create a Stripe Checkout session for a completed booking.
     * Returns the hosted payment URL.
     */
    @Transactional
    public String createCheckoutSession(UUID bookingId) {
        return createCheckoutSession(bookingId, null, true);
    }

    @Transactional
    public String createCheckoutSession(UUID bookingId, UUID requesterId, boolean isAdmin) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new NotFoundException("Booking not found: " + bookingId));
        verifyBookingAccess(booking, requesterId, isAdmin);

        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new BadRequestException("Booking is not awaiting payment (status: " + booking.getStatus() + ")");
        }

        Bill bill = booking.getBill();
        if (bill == null) {
            throw new BadRequestException("No bill found for booking: " + bookingId);
        }

        // Idempotency: return existing link if already created
        return paymentRepository.findByBillId(bill.getId())
            .filter(p -> p.getPaymentLink() != null)
            .map(Payment::getPaymentLink)
            .orElseGet(() -> createStripeSession(booking, bill));
    }

    private String createStripeSession(Booking booking, Bill bill) {
        if (isMockStripeMode()) {
            return createMockCheckoutSession(booking, bill);
        }

        try {
            long amountCents = bill.getTotalAmount()
                .multiply(java.math.BigDecimal.valueOf(100))
                .longValue();

            SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl + "?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(cancelUrl + "?booking_id=" + booking.getId())
                .addLineItem(SessionCreateParams.LineItem.builder()
                    .setQuantity(1L)
                    .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                        .setCurrency("usd")
                        .setUnitAmount(amountCents)
                        .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                            .setName("Parking Fee — " + booking.getVehicle().getLicensePlate())
                            .setDescription("Booking ID: " + booking.getId()
                                + " | Duration: " + booking.getDurationMinutes() + " mins")
                            .build())
                        .build())
                    .build())
                .putMetadata("bookingId", booking.getId().toString())
                .putMetadata("billId", bill.getId().toString())
                .build();

            Session session = Session.create(params);

            Payment payment = Payment.builder()
                .bill(bill)
                .amount(bill.getTotalAmount())
                .stripeSessionId(session.getId())
                .paymentLink(session.getUrl())
                .status(PaymentStatus.PENDING)
                .build();

            paymentRepository.save(payment);
            log.info("Stripe session created for booking {}: {}", booking.getId(), session.getId());
            return session.getUrl();

        } catch (Exception e) {
            log.error("Stripe session creation failed for booking {}: {}", booking.getId(), e.getMessage());
            throw new RuntimeException("Payment session creation failed: " + e.getMessage(), e);
        }
    }

    private boolean isMockStripeMode() {
        return !StringUtils.hasText(stripeSecretKey)
            || "sk_test_placeholder".equals(stripeSecretKey);
    }

    private String createMockCheckoutSession(Booking booking, Bill bill) {
        String sessionId = "cs_mock_" + booking.getId();
        String paymentLink = successUrl + "?session_id=" + sessionId;

        Payment payment = Payment.builder()
            .bill(bill)
            .amount(bill.getTotalAmount())
            .stripeSessionId(sessionId)
            .paymentLink(paymentLink)
            .status(PaymentStatus.PENDING)
            .build();

        paymentRepository.save(payment);
        log.warn("Using mock Stripe checkout session for booking {}", booking.getId());
        return paymentLink;
    }

    /**
     * Handle Stripe webhook — verify signature, update payment status.
     */
    @Transactional
    public void handleWebhook(String payload, String sigHeader) {
        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.warn("Invalid Stripe webhook signature");
            throw new BadRequestException("Invalid webhook signature");
        }

        log.info("Received Stripe event: {}", event.getType());

        if ("checkout.session.completed".equals(event.getType())) {
            Session session = (Session) event.getDataObjectDeserializer()
                .getObject().orElseThrow();
            onPaymentCompleted(session);
        } else if ("checkout.session.expired".equals(event.getType())) {
            Session session = (Session) event.getDataObjectDeserializer()
                .getObject().orElseThrow();
            onPaymentExpired(session);
        }
    }

    private void onPaymentCompleted(Session session) {
        paymentRepository.findByStripeSessionId(session.getId()).ifPresent(payment -> {
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setStripePaymentIntent(session.getPaymentIntent());
            payment.setPaidAt(java.time.LocalDateTime.now());
            paymentRepository.save(payment);

            Booking booking = payment.getBill().getBooking();
            booking.setStatus(BookingStatus.COMPLETED);
            bookingRepository.save(booking);

            log.info("Payment completed for booking {}", booking.getId());
            eventPublisher.publish(
                BookingEvent.from(booking, BookingEvent.Type.PAYMENT_COMPLETED)
                    .totalAmount(payment.getBill().getTotalAmount())
                    .paymentLink(payment.getPaymentLink())
                    .build()
            );
        });
    }

    private void onPaymentExpired(Session session) {
        paymentRepository.findByStripeSessionId(session.getId()).ifPresent(payment -> {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);

            Booking booking = payment.getBill().getBooking();
            log.warn("Payment expired for booking {}", booking.getId());
            eventPublisher.publish(
                BookingEvent.from(booking, BookingEvent.Type.PAYMENT_FAILED)
                    .totalAmount(payment.getBill().getTotalAmount())
                    .paymentLink(payment.getPaymentLink())
                    .build()
            );
        });
    }

    @Transactional(readOnly = true)
    public Payment getPaymentStatus(UUID bookingId) {
        return getPaymentStatus(bookingId, null, true);
    }

    @Transactional(readOnly = true)
    public Payment getPaymentStatus(UUID bookingId, UUID requesterId, boolean isAdmin) {
        Payment payment = paymentRepository.findByBillBookingId(bookingId)
            .orElseThrow(() -> new NotFoundException("No payment found for booking: " + bookingId));
        verifyBookingAccess(payment.getBill().getBooking(), requesterId, isAdmin);
        return payment;
    }

    private void verifyBookingAccess(Booking booking, UUID requesterId, boolean isAdmin) {
        if (isAdmin) return;
        if (booking.getUser() == null || !booking.getUser().getId().equals(requesterId)) {
            throw new AccessDeniedException("Booking does not belong to this user");
        }
    }
}
