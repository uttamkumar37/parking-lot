package com.parksmart.config;

import com.parksmart.event.BookingEventPublisher;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaConfigTest {

    @Test
    void bookingTopic_usesPublisherTopicName() {
        KafkaConfig config = new KafkaConfig();

        assertThat(config.bookingTopic().name())
            .isEqualTo(BookingEventPublisher.BOOKING_TOPIC);
    }
}
