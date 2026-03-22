package com.parksmart.service;

import com.parksmart.event.BookingEvent;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class NotificationService {

    private final JavaMailSender mailSender;

    @Value("${notification.from-email}")
    private String fromEmail;

    @Value("${notification.from-name}")
    private String fromName;

    @Value("${twilio.account-sid:}")
    private String twilioAccountSid;

    @Value("${twilio.auth-token:}")
    private String twilioAuthToken;

    @Value("${twilio.from-number:}")
    private String twilioFromNumber;

    public NotificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @PostConstruct
    public void initTwilio() {
        if (StringUtils.hasText(twilioAccountSid) && StringUtils.hasText(twilioAuthToken)) {
            Twilio.init(twilioAccountSid, twilioAuthToken);
            log.info("Twilio initialized");
        } else {
            log.warn("Twilio credentials not configured — SMS notifications disabled");
        }
    }

    // ─── Public API ──────────────────────────────────────────────────────────────

    @Async
    public void sendBookingConfirmation(BookingEvent event) {
        String subject = "Booking Confirmed – ParkSmart #" + shortId(event.getBookingId());
        String body = """
            Hi %s,

            Your vehicle %s has been parked successfully.

            Booking ID : %s
            Slot       : %s (Floor: %s)
            Lot        : %s
            Entry Time : %s

            Safe travels!
            – ParkSmart Team
            """.formatted(
                event.getUserName(),
                event.getLicensePlate(),
                event.getBookingId(),
                event.getSlotNumber(),
                event.getFloorName(),
                event.getLotName(),
                event.getEntryTime()
            );

        sendEmail(event.getUserEmail(), subject, body);
        sendSms(event.getUserPhone(),
            "ParkSmart: Vehicle " + event.getLicensePlate() + " parked at slot "
                + event.getSlotNumber() + ", " + event.getLotName() + ". Booking: " + shortId(event.getBookingId()));
    }

    @Async
    public void sendBillGenerated(BookingEvent event) {
        String subject = "Your ParkSmart Bill – #" + shortId(event.getBookingId());
        String body = """
            Hi %s,

            Your parking session has ended.

            Booking ID     : %s
            Vehicle        : %s
            Duration       : %s minutes
            Amount Due     : $%s

            Pay now (link valid for 30 min): %s

            – ParkSmart Team
            """.formatted(
                event.getUserName(),
                event.getBookingId(),
                event.getLicensePlate(),
                event.getDurationMinutes(),
                event.getTotalAmount(),
                event.getPaymentLink() != null ? event.getPaymentLink() : "N/A"
            );

        sendEmail(event.getUserEmail(), subject, body);
        if (event.getPaymentLink() != null) {
            sendSms(event.getUserPhone(),
                "ParkSmart: Bill ready for booking " + shortId(event.getBookingId())
                    + ". Amount: $" + event.getTotalAmount()
                    + ". Pay: " + event.getPaymentLink());
        }
    }

    @Async
    public void sendPaymentReceipt(BookingEvent event) {
        String subject = "Payment Received – ParkSmart #" + shortId(event.getBookingId());
        String body = """
            Hi %s,

            Payment confirmed for booking #%s.

            Amount Paid : $%s
            Vehicle     : %s

            Thank you for using ParkSmart!
            – ParkSmart Team
            """.formatted(
                event.getUserName(),
                shortId(event.getBookingId()),
                event.getTotalAmount(),
                event.getLicensePlate()
            );

        sendEmail(event.getUserEmail(), subject, body);
        sendSms(event.getUserPhone(),
            "ParkSmart: Payment of $" + event.getTotalAmount()
                + " confirmed for booking " + shortId(event.getBookingId()) + ". Thank you!");
    }

    @Async
    public void sendPaymentFailed(BookingEvent event) {
        String subject = "Payment Failed – ParkSmart Action Required";
        String body = """
            Hi %s,

            Your payment for booking #%s could not be processed.

            Please retry: %s

            If you need help, contact support@parksmart.com.
            – ParkSmart Team
            """.formatted(
                event.getUserName(),
                shortId(event.getBookingId()),
                event.getPaymentLink() != null ? event.getPaymentLink() : "N/A"
            );

        sendEmail(event.getUserEmail(), subject, body);
        sendSms(event.getUserPhone(),
            "ParkSmart: Payment failed for booking " + shortId(event.getBookingId())
                + ". Retry: " + event.getPaymentLink());
    }

    // ─── Internal helpers ────────────────────────────────────────────────────────

    private void sendEmail(String to, String subject, String body) {
        if (!StringUtils.hasText(to)) {
            log.warn("Cannot send email — recipient address is empty");
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromName + " <" + fromEmail + ">");
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            mailSender.send(msg);
            log.info("Email sent to {} | subject: {}", to, subject);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    private void sendSms(String to, String message) {
        if (!StringUtils.hasText(to) || !StringUtils.hasText(twilioAccountSid)) {
            log.debug("SMS skipped — phone={}, twilioConfigured={}", to, StringUtils.hasText(twilioAccountSid));
            return;
        }
        try {
            Message.creator(new PhoneNumber(to), new PhoneNumber(twilioFromNumber), message)
                .create();
            log.info("SMS sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send SMS to {}: {}", to, e.getMessage());
        }
    }

    private String shortId(Object id) {
        if (id == null) return "N/A";
        String s = id.toString();
        return s.length() > 8 ? s.substring(0, 8) : s;
    }
}
