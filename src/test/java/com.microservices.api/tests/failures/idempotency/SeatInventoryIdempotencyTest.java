package com.microservices.api.tests.failures.idempotency;

import com.microservices.api.model.events.SeatReservedEvent;
import com.microservices.api.model.request.BookingRequest;
import com.microservices.api.model.response.BookingResponse;
import com.microservices.api.tests.base.BaseKafkaIntegrationTest;
import com.microservices.api.util.KafkaTestAssertions;
import com.microservices.api.util.KafkaTestUtils;
import com.microservices.api.util.TestDataCleaner;
import com.microservices.api.util.TestDataSeeder;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.kafka.clients.consumer.Consumer;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.microservices.api.util.DBHelper.*;
import static org.testng.AssertJUnit.assertEquals;

/*1. Simple idempotency
   2.Partial seat duplicate
   3.Out-of-order event
   4.Multiple duplicates
   5.Event without booking
   6.Idempotent rollback*/

public class SeatInventoryIdempotencyTest extends BaseKafkaIntegrationTest {

    private Consumer<String, SeatReservedEvent> seatConsumer;
    private static final String SEAT_TOPIC = "seat-reserved-topic";
    private String reservationId;

    @BeforeClass
    void setup() throws Exception {
        seatConsumer = createConsumer("seat-idempotency-group", SeatReservedEvent.class, SEAT_TOPIC);
        KafkaTestUtils.deleteTopicRecords("localhost:9092", SEAT_TOPIC);
    }

    /**
     * 1️⃣ Simple idempotency for a failed booking
     */
    @Test(enabled = false)
    public void seat_reserved_event_should_be_idempotent() throws Exception {
        List<String> seats = List.of("B1");
        String bookingId = UUID.randomUUID().toString();
        TestDataSeeder.lockSeat("SHOW_1", "B1", "OTHER_BOOKING");

        createBooking(bookingId, seats);

        KafkaTestAssertions.assertIdempotentEvent(
                seatConsumer,
                SEAT_TOPIC,
                reservationId,
                3, // simulate 3 duplicates
                SeatReservedEvent::getBookingId,
                () -> {
                    try {
                        assertSeatLocked("SHOW_1", "B1", "OTHER_BOOKING");
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                },
                "FAILED"
        );
    }

    /**
     * 2️⃣ Partial seat reservation duplicate handling
     */
    @Test(enabled = false)
    public void partial_seat_reserved_event_should_be_idempotent() throws Exception {
        List<String> seats = List.of("B2", "B3");
        String bookingId = UUID.randomUUID().toString();

        TestDataSeeder.lockSeat("SHOW_1", "B2", "OTHER_BOOKING");

        createBooking(bookingId, seats);

        KafkaTestAssertions.assertIdempotentEvent(
                seatConsumer,
                SEAT_TOPIC,
                reservationId,
                2,
                SeatReservedEvent::getBookingId,
                () -> {
                    try {
                        assertSeatLocked("SHOW_1", "B2", "OTHER_BOOKING");
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                    try {
                        assertSeatAvailable("SHOW_1", "B3");
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                },
                "FAILED"
        );
    }

    /**
     * 3️⃣ Out-of-order event handling - NOT WORKING
     */
    @Test(enabled = false)
    public void seat_reserved_event_out_of_order_should_be_handled_safely() throws Exception {
        List<String> seats = List.of("B4");
        String bookingId = UUID.randomUUID().toString();

        // 1️⃣ Create the booking first — DB now has the record and seat is locked
        createBooking(bookingId, seats);

        // 2️⃣ Publish an "out-of-order" SeatReservedEvent for the same booking
        SeatReservedEvent outOfOrderEvent = new SeatReservedEvent(bookingId, true, 500);

        // Explicitly publish multiple duplicates to simulate retries/out-of-order delivery
        for (int i = 0; i < 3; i++) {
            KafkaTestUtils.publishEvent(SEAT_TOPIC, reservationId, outOfOrderEvent);
        }

        // 3️⃣ Poll consumer and assert idempotency — no side-effects should occur
        KafkaTestAssertions.assertNoSideEffectsAfterDuplicateEvents(
                seatConsumer,
                SEAT_TOPIC,
                reservationId,
                SeatReservedEvent::getBookingId,
                () -> {
                    try {
                        // ✅ Seat should remain locked by this booking
                        assertSeatLocked("SHOW_1", "B4", bookingId);
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                },
                "FAILED" // Expected booking status after failed reservation attempt
        );
    }



    /**
     * 4️⃣ Multiple duplicate events
     */
    @Test(enabled = false)
    public void seat_reserved_event_multiple_duplicates_should_be_idempotent() throws Exception {
        List<String> seats = List.of("B5");
        String bookingId = UUID.randomUUID().toString();

        TestDataSeeder.lockSeat("SHOW_1", "B5", "OTHER_BOOKING");

        createBooking(bookingId, seats);

        // ✅ Explicit duplicate events
        SeatReservedEvent duplicateEvent = new SeatReservedEvent(bookingId, false, 500);

        for (int i = 0; i < 3; i++) {
            KafkaTestUtils.publishEvent(
                    SEAT_TOPIC,
                    reservationId,
                    duplicateEvent
            );
        }

        // ✅ Assert idempotency
        KafkaTestAssertions.assertNoSideEffectsAfterDuplicateEvents(
                seatConsumer,
                SEAT_TOPIC,
                reservationId,
                SeatReservedEvent::getBookingId,
                () -> {
                    try {
                        assertSeatLocked("SHOW_1", "B5", "OTHER_BOOKING");
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                },
                "FAILED"
        );
    }


    /**
     * 5️⃣ Event without corresponding booking
     */
    @Test(enabled = false)
    public void seat_reserved_event_without_booking_should_be_ignored() throws Exception {
        List<String> seats = List.of("B6");
        String fakeBookingId = UUID.randomUUID().toString();

        SeatReservedEvent fakeEvent = new SeatReservedEvent(fakeBookingId, true, 500);
       // KafkaTestUtils.publishEvent("localhost:9092", SEAT_TOPIC, fakeBookingId, fakeEvent);
        KafkaTestUtils.publishEvent( SEAT_TOPIC, fakeBookingId, fakeEvent);
        // Poll consumer; should eventually timeout without errors
        SeatReservedEvent seatEvent = pollForBookingEvent(seatConsumer, SEAT_TOPIC, Duration.ofSeconds(5), fakeBookingId, SeatReservedEvent::getBookingId);

        assertSeatAvailable("SHOW_1", "B6");
    }

    /**
     * 6️⃣ Idempotent seat release / rollback handling - NOT WORKING
     */
    @Test(enabled = false)
    public void seat_release_event_should_be_idempotent() throws Exception {
        List<String> seats = List.of("B7");
        String bookingId = UUID.randomUUID().toString();

        TestDataSeeder.lockSeat("SHOW_1", "B7", "OTHER_BOOKING");

        createBooking(bookingId, seats);

        KafkaTestAssertions.assertIdempotentEvent(
                seatConsumer,
                SEAT_TOPIC,
                reservationId,
                3, // simulate 3 release duplicates
                SeatReservedEvent::getBookingId,
                () -> {
                    try {
                        assertSeatAvailable("SHOW_1", "B7");
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                },
                "FAILED"
        );
    }

    @AfterClass
    void tearDown() throws SQLException {
        TestDataCleaner.releaseAllSeatsForShow("SHOW_1");
        seatConsumer.close();
        KafkaTestUtils.shutdown();
    }

    /**
     * Helper method to create a booking
     */
    private void createBooking(String bookingId, List<String> seats) {
        BookingRequest request = new BookingRequest(UUID.randomUUID().toString(), "SHOW_1", seats, bookingId, Instant.now(), 500);
        BookingResponse response=RestAssured.given()
                .contentType(ContentType.JSON)
                .body(request)
                .post("http://localhost:9191/booking-service/bookSeat")
                .then().statusCode(200).extract()
                .as(BookingResponse.class);

        reservationId = response.getReservationId();;
    }
}
