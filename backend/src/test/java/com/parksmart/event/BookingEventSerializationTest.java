package com.parksmart.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BookingEventSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule());

    @Test
    void bookingEvent_roundTripsThroughJson() throws Exception {
        UUID bookingId = UUID.randomUUID();

        BookingEvent event = new BookingEvent.Builder()
            .bookingId(bookingId)
            .userId(UUID.randomUUID())
            .slotId(UUID.randomUUID())
            .lotId(UUID.randomUUID())
            .eventType(BookingEvent.Type.BOOKING_COMPLETED)
            .occurredAt(LocalDateTime.now())
            .userEmail("driver@example.com")
            .userPhone("+15551234567")
            .userName("Driver One")
            .licensePlate("ABC123")
            .slotNumber("G-M01")
            .floorName("Ground Floor")
            .lotName("ParkSmart Downtown")
            .entryTime(LocalDateTime.now().minusHours(1))
            .durationMinutes(60L)
            .paymentLink("http://localhost:3000/payment/success?session_id=cs_mock")
            .totalAmount(new BigDecimal("5.72"))
            .build();

        String json = objectMapper.writeValueAsString(event);
        BookingEvent restored = objectMapper.readValue(json, BookingEvent.class);

        assertThat(restored.getBookingId()).isEqualTo(bookingId);
        assertThat(restored.getEventType()).isEqualTo(BookingEvent.Type.BOOKING_COMPLETED);
        assertThat(restored.getPaymentLink()).isEqualTo(event.getPaymentLink());
        assertThat(restored.getTotalAmount()).isEqualByComparingTo("5.72");
    }
}
