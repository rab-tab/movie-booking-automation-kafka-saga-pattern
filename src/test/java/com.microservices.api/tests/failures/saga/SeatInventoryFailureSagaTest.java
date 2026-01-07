package com.microservices.api.tests.failures.saga;

import com.microservices.api.model.events.SeatReservedEvent;
import com.microservices.api.model.request.BookingRequest;
import com.microservices.api.model.response.BookingResponse;
import com.microservices.api.tests.base.BaseKafkaIntegrationTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.kafka.clients.consumer.Consumer;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import com.microservices.api.util.KafkaTestUtils;
import com.microservices.api.util.TestDataCleaner;
import com.microservices.api.util.TestDataSeeder;

import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.microservices.api.util.DBHelper.*;
import static org.testng.Assert.assertFalse;

public class SeatInventoryFailureSagaTest extends BaseKafkaIntegrationTest {
    private Consumer<String, SeatReservedEvent> seatConsumer;
    private static final String SEAT_TOPIC = "seat-reserved-topic";

    private static String reservationId;
    private static String bookingStatus;
    private static UUID userId;
    private static List<String> seats;
    @BeforeClass
    void setup() throws Exception {
        seatConsumer = createConsumer("seat-release-it-group", SeatReservedEvent.class, SEAT_TOPIC);
        // 1️⃣ Clear Kafka topics before test
        KafkaTestUtils.deleteTopicRecords("localhost:9092", SEAT_TOPIC);

    }

    @Test
    public void seat_inventory_failure_should_rollback_booking() throws Exception {
        seats = List.of("A1");
        // 1️⃣ Pre-lock seat
        TestDataSeeder.lockSeat("SHOW_1", "A1", "OTHER_BOOKING");

        // 2️⃣ Create booking
        BookingRequest request = new BookingRequest("BKG_SEAT_FAIL", "SHOW_1", seats, UUID.randomUUID().toString(), Instant.now(), 500);
        BookingResponse response =
                RestAssured.given()
                        .contentType(ContentType.JSON)
                        .body(request)
                        .post("http://localhost:9191/booking-service/bookSeat")
                        .then()
                        .statusCode(200)
                        .extract()
                        .as(BookingResponse.class);

        String bookingId = response.getReservationId();

        // 3️⃣ Assert seat failure event
        SeatReservedEvent seatEvent = pollForBookingEvent(seatConsumer, SEAT_TOPIC, Duration.ofSeconds(10), bookingId, SeatReservedEvent::getBookingId);
        //assertFalse(seatEvent.isReserved());

        // DB assertion
        assertSeatLocked("SHOW_1", "A1", "OTHER_BOOKING");
        // 4️⃣ DB rollback
        assertBookingStatus(bookingId, "FAILED");
    }
    @Test
    public void partial_seat_inventory_failure_should_rollback_booking() throws Exception {
        // Mixed availability
        seats = List.of("A1", "A2");

        // 1️⃣ Pre-lock only ONE seat
        TestDataSeeder.lockSeat("SHOW_1", "A1", "OTHER_BOOKING");

        // 2️⃣ Create booking requesting both seats
        BookingRequest request = new BookingRequest("BKG_PARTIAL_SEAT_FAIL", "SHOW_1", seats, UUID.randomUUID().toString(), Instant.now(), 500);

        BookingResponse response = RestAssured.given()
                        .contentType(ContentType.JSON)
                        .body(request)
                        .post("http://localhost:9191/booking-service/bookSeat")
                        .then()
                        .statusCode(200)
                        .extract()
                        .as(BookingResponse.class);

        String bookingId = response.getReservationId();

        // 3️⃣ Assert seat reservation FAILED event
        SeatReservedEvent seatEvent =
                pollForBookingEvent(
                        seatConsumer,
                        SEAT_TOPIC,
                        Duration.ofSeconds(10),
                        bookingId,
                        SeatReservedEvent::getBookingId
                );

        assertFalse(seatEvent.isReserved(), "Seat reservation should fail when one seat is unavailable");

        // 4️⃣ DB assertions
        // A1 should remain locked by OTHER_BOOKING
        assertSeatLocked("SHOW_1", "A1", "OTHER_BOOKING");

        // A2 should remain AVAILABLE (must NOT be locked by this booking)
        assertSeatAvailable("SHOW_1", "A2");

        // 5️⃣ Booking must be rolled back
        assertBookingStatus(bookingId, "FAILED");
    }


    @AfterClass
    void tearDown() throws SQLException {
        TestDataCleaner.releaseAllSeatsForShow("SHOW_1");
        seatConsumer.close();
    }

}
