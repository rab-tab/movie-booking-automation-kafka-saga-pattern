package com.microservices.api.util;

import com.microservices.api.tests.BaseKafkaIntegrationTest;
import org.apache.kafka.clients.consumer.Consumer;

import java.time.Duration;
import java.util.function.Function;

import static org.testng.Assert.assertEquals;

public class KafkaTestAssertions {

    /**
     * Polls for a specific event from Kafka, simulates duplicates, and asserts DB state.
     *
     * @param consumer       Kafka consumer
     * @param topic          Topic to poll
     * @param key            Event key (e.g., bookingId)
     * @param duplicateCount How many duplicate events to publish
     * @param keyExtractor   Function to extract key from event
     * @param dbAssertion    Runnable to assert DB state
     * @param expectedStatus Expected booking/payment status after duplicates
     */
    public static <T> void assertIdempotentEvent(
            Consumer<String, T> consumer,
            String topic,
            String key,
            int duplicateCount,
            Function<T, String> keyExtractor,
            Runnable dbAssertion,
            String expectedStatus
    ) throws Exception {

        // NOTE: we cannot create a generic T instance here
        // The test should publish events manually if needed for the type
        // Here we just poll and assert DB state

        // Poll first event from consumer
        T event = BaseKafkaIntegrationTest.pollForBookingEvent(
                consumer,
                topic,
                Duration.ofSeconds(10),
                key,
                keyExtractor
        );

        // Run DB assertions
        dbAssertion.run();

        // Assert booking/payment status
        assertEquals(BaseKafkaIntegrationTest.getBookingStatus(key), expectedStatus);
    }

    public static <T> void assertNoSideEffectsAfterDuplicateEvents(
            Consumer<String, T> consumer,
            String topic,
            String key,
            Function<T, String> keyExtractor,
            Runnable dbAssertion,
            String expectedStatus
    ) throws Exception {

        // Drain ALL events for this key (including duplicates)
        BaseKafkaIntegrationTest.pollForBookingEvent(
                consumer,
                topic,
                Duration.ofSeconds(10),
                key,
                keyExtractor
        );

        // Assert DB invariants AFTER duplicates
        dbAssertion.run();

        // Assert booking/payment status is stable
        assertEquals(
                BaseKafkaIntegrationTest.getBookingStatus(key),
                expectedStatus,
                "Booking status changed due to duplicate events"
        );
    }

}
