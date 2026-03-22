package com.parksmart.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka-backed event publisher.
 * Services call this to emit domain events asynchronously.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BookingEventPublisher {

    public static final String BOOKING_TOPIC = "parking.booking.events";

    private final KafkaTemplate<String, BookingEvent> kafkaTemplate;

    public void publish(BookingEvent event) {
        log.info("Publishing event {} for booking {}", event.getEventType(), event.getBookingId());
        kafkaTemplate.send(BOOKING_TOPIC, event.getBookingId().toString(), event);
    }
}
