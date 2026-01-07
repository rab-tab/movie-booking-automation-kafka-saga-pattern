package com.microservices.api.tests.failures.idempotency;

import com.microservices.api.model.events.BookingPaymentEvent;
import com.microservices.api.model.request.BookingRequest;
import com.microservices.api.model.response.BookingResponse;
import com.microservices.api.tests.base.BaseKafkaIntegrationTest;
import com.microservices.api.util.KafkaTestAssertions;
import com.microservices.api.util.KafkaTestUtils;
import com.microservices.api.util.TestDataCleaner;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.kafka.clients.consumer.Consumer;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;


import static com.microservices.api.util.DBHelper.assertPaymentStatus;
import static org.testng.Assert.assertEquals;

public class PaymentIdempotencyTest extends BaseKafkaIntegrationTest {

    private Consumer<String, BookingPaymentEvent> paymentConsumer;
    private static final String PAYMENT_TOPIC = "payment-events";

    @BeforeClass
    void setup() throws Exception {
        paymentConsumer = createConsumer("payment-idempotency-group", BookingPaymentEvent.class, PAYMENT_TOPIC);
        KafkaTestUtils.deleteTopicRecords("localhost:9092", PAYMENT_TOPIC);
    }

    /**
     * 1️⃣ Simple duplicate payment event
     */
    @Test
    public void payment_event_should_be_idempotent() throws Exception {
        String bookingId = UUID.randomUUID().toString();
        createBooking(bookingId, List.of("D1"));

        KafkaTestAssertions.assertIdempotentEvent(
                paymentConsumer,
                PAYMENT_TOPIC,
                bookingId,
                3,
                event -> event.getBookingId(), // lambda fixes the "non-static" error
                () -> assertPaymentStatus(bookingId, "CONFIRMED"),
                "CONFIRMED"
        );
    }

    /**
     * 2️⃣ Partial payment / multiple seats
     */
    @Test
    public void partial_payment_event_should_be_idempotent() throws Exception {
        String bookingId = UUID.randomUUID().toString();
        createBooking(bookingId, List.of("D2", "D3"));

        KafkaTestAssertions.assertIdempotentEvent(
                paymentConsumer,
                PAYMENT_TOPIC,
                bookingId,
                2,
                event -> event.getBookingId(),
                () -> assertPaymentStatus(bookingId, "CONFIRMED"),
                "CONFIRMED"
        );
    }

    /**
     * 3️⃣ Out-of-order payment event - INVALID TEST
     */
    @Test
    public void payment_event_out_of_order_should_be_handled() throws Exception {
        String bookingId = UUID.randomUUID().toString();

        // Publish payment event before booking
        BookingPaymentEvent preEvent = new BookingPaymentEvent(bookingId, true, 500);
       // KafkaTestUtils.publishEvent("localhost:9092", PAYMENT_TOPIC, bookingId, preEvent);
        KafkaTestUtils.publishEvent( PAYMENT_TOPIC, bookingId, preEvent);

        createBooking(bookingId, List.of("D4"));

        KafkaTestAssertions.assertIdempotentEvent(
                paymentConsumer,
                PAYMENT_TOPIC,
                bookingId,
                0,
                event -> event.getBookingId(),
                () -> assertPaymentStatus(bookingId, "CONFIRMED"),
                "CONFIRMED"
        );
    }

    /**
     * 4️⃣ Multiple duplicate payment events
     */
    @Test
    public void payment_event_multiple_duplicates_should_be_idempotent() throws Exception {
        String bookingId = UUID.randomUUID().toString();
        createBooking(bookingId, List.of("D5"));

        KafkaTestAssertions.assertIdempotentEvent(
                paymentConsumer,
                PAYMENT_TOPIC,
                bookingId,
                5,
                event -> event.getBookingId(),
                () -> assertPaymentStatus(bookingId, "CONFIRMED"),
                "CONFIRMED"
        );
    }

    /**
     * 5️⃣ Event for non-existent booking - INVALID TEST
     */
    @Test
    public void payment_event_without_booking_should_be_ignored() throws Exception {
        String fakeBookingId = UUID.randomUUID().toString();

        BookingPaymentEvent fakeEvent = new BookingPaymentEvent(fakeBookingId, true, 500);
        KafkaTestUtils.publishEvent(PAYMENT_TOPIC, fakeBookingId, fakeEvent);

        // Poll consumer; should timeout gracefully
        BookingPaymentEvent seatEvent = pollForBookingEvent(paymentConsumer, PAYMENT_TOPIC, java.time.Duration.ofSeconds(5), fakeBookingId, BookingPaymentEvent::getBookingId);

        // Ensure no payment record exists
        assertPaymentStatus(fakeBookingId, null);
    }

    /**
     * 6️⃣ Idempotent rollback / refund duplicates - INVALID TEST
     */
    @Test
    public void payment_rollback_event_should_be_idempotent() throws Exception {
        String bookingId = UUID.randomUUID().toString();
        createBooking(bookingId, List.of("D6"));

        // Simulate failed payment events multiple times
        KafkaTestAssertions.assertIdempotentEvent(
                paymentConsumer,
                PAYMENT_TOPIC,
                bookingId,
                3,
                event -> event.getBookingId(),
                () -> assertPaymentStatus(bookingId, "FAILED"),
                "FAILED"
        );
    }

    @AfterClass
    void tearDown() throws SQLException {
        paymentConsumer.close();
        TestDataCleaner.releaseAllSeatsForShow("SHOW_1");
    }

    /**
     * Helper to create a booking with payment
     */
    private void createBooking(String bookingId, List<String> seats) {
        BookingRequest request = new BookingRequest(UUID.randomUUID().toString(), "SHOW_1", seats, bookingId, Instant.now(), 500);
        BookingResponse response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(request)
                .post("http://localhost:9191/booking-service/bookSeat")
                .then().statusCode(200)
                .extract().as(BookingResponse.class);

        assertEquals(response.getReservationId(), bookingId);
    }
}
