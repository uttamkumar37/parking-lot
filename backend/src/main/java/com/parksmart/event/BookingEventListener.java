package com.parksmart.event;

import com.parksmart.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer: listens to booking events and triggers notifications.
 * Scales independently from the booking service.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BookingEventListener {

    private final NotificationService notificationService;

    @KafkaListener(
        topics = BookingEventPublisher.BOOKING_TOPIC,
        groupId = "notification-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onBookingEvent(BookingEvent event) {
        log.info("Received event {} for booking {}", event.getEventType(), event.getBookingId());
        try {
            switch (event.getEventType()) {
                case BOOKING_CREATED   -> notificationService.sendBookingConfirmation(event);
                case BOOKING_COMPLETED -> notificationService.sendBillGenerated(event);
                case PAYMENT_COMPLETED -> notificationService.sendPaymentReceipt(event);
                case PAYMENT_FAILED    -> notificationService.sendPaymentFailed(event);
                default -> log.debug("No notification handler for event type: {}", event.getEventType());
            }
        } catch (Exception e) {
            log.error("Failed to process notification for event {}: {}", event.getEventType(), e.getMessage(), e);
            // In production: publish to DLQ / retry with exponential backoff
        }
    }
}
