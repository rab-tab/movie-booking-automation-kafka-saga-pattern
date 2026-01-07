package com.microservices.api.tests.failures.resiliency;

import com.microservices.api.model.common.KafkaConfigProperties;
import com.microservices.api.model.events.BookingCreatedEvent;
import com.microservices.api.model.request.BookingRequest;
import com.microservices.api.model.response.BookingResponse;
import com.microservices.api.tests.base.BaseKafkaIntegrationTest;
import com.microservices.api.util.KafkaTestUtils;
import com.microservices.api.util.TestDataCleaner;
import exception.NonRecoverableBusinessException;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.support.KafkaHeaders;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.microservices.api.util.DBHelper.assertSeatAvailable;
import static com.microservices.api.util.DBHelper.assertSeatLocked;
import static org.mockito.ArgumentMatchers.any;
import static org.testng.Assert.*;


public class BookingEventResilienceTest extends BaseKafkaIntegrationTest {

    private String reservationId;
    @BeforeClass
    void setup() throws Exception {
        // Clean seats for consistent state
        TestDataCleaner.releaseAllSeatsForShow("SHOW_1");
    }

    /**
     * Scenario: Seat Inventory Service is DOWN
     * Booking should remain PENDING and seat should NOT be locked
     */
    @Test(enabled = false)
    public void booking_should_fail_if_seat_service_down() throws Exception {
        List<String> seats = List.of("B10");
        String bookingId = UUID.randomUUID().toString();

        // 1️⃣ Pause Seat Inventory Listener via control endpoint
        RestAssured.given().log().all()
                .post("http://localhost:9292/kafka/pause/seat-booking-created-listener")
                .then().log().all().statusCode(200);

        // 2️⃣ Create a booking
        BookingRequest request = new BookingRequest(UUID.randomUUID().toString(), "SHOW_1", seats, bookingId, Instant.now(), 500);
        BookingResponse response = RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .body(request)
                .post("http://localhost:9191/booking-service/bookSeat")
                .then().log().all().statusCode(200)
                .extract().as(BookingResponse.class);
        reservationId = response.getReservationId();;


        // 3️⃣ Wait a few seconds for async processing (which won't happen since listener is paused)
        Thread.sleep(5000);

        // 4️⃣ Assert booking is null
        String bookingStatus = getBookingStatus(reservationId);
        assertNull(bookingStatus);


        // 5️⃣ Assert seat is still AVAILABLE
        assertSeatAvailable("SHOW_1", "B10");

        // 6️⃣ Resume Seat Inventory Listener for cleanup
        RestAssured.given().log().all()
                .post("http://localhost:9292/kafka/resume/seat-booking-created-listener")
                .then().log().all().statusCode(200);
    }

    @Test(enabled = false)
    public void booking_should_remain_pending_if_payment_service_down() throws Exception {

        List<String> seats = List.of("B11");
        String bookingId = UUID.randomUUID().toString();

        // 1️⃣ Pause Payment Service listener
        RestAssured.given().log().all()
                .post("http://localhost:9292/kafka/pause/seat-payment-status-listener")
                .then().log().all().statusCode(200);

        // 2️⃣ Create booking
        BookingRequest request = new BookingRequest(
                UUID.randomUUID().toString(),
                "SHOW_1",
                seats,
                bookingId,
                Instant.now(),
                500
        );

        BookingResponse response = RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .body(request)
                .post("http://localhost:9191/booking-service/bookSeat")
                .then().log().all().statusCode(200)
                .extract().as(BookingResponse.class);

        reservationId = response.getReservationId();

        // 3️⃣ Wait for async flow (seat will lock, payment won't happen)
        Thread.sleep(5000);

        // 4️⃣ Booking should be PENDING (seat reserved, payment missing)
        String bookingStatus = getBookingStatus(reservationId);
        assertEquals("PENDING", bookingStatus);

        // 5️⃣ Seat should remain LOCKED
        assertSeatLocked("SHOW_1", "B11", reservationId);

        // 6️⃣ Resume Payment listener for cleanup
        RestAssured.given().log().all()
                .post("http://localhost:9292/kafka/resume/seat-payment-status-listener")
                .then().log().all().statusCode(200);
    }

    @Test(enabled = false)
    public void booking_should_retry_and_go_to_dlt_on_seat_service_timeout() throws Exception {

        List<String> seats = List.of("B10");
        String bookingId = UUID.randomUUID().toString();

        // 1️⃣ Enable timeout simulation in seat-inventory-service
        RestAssured.given()
                .post("http://localhost:9292/internal/test/seat-inventory/timeout/enable")
                .then()
                .statusCode(200);

        // 2️⃣ Create booking (publishes BookingCreatedEvent)
        BookingRequest request = new BookingRequest(
                UUID.randomUUID().toString(),
                "SHOW_1",
                seats,
                bookingId,
                Instant.now(),
                500
        );

        BookingResponse response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(request)
                .post("http://localhost:9191/booking-service/bookSeat")
                .then()
                .statusCode(200)
                .extract()
                .as(BookingResponse.class);

        String reservationId = response.getReservationId();

        // 3️⃣ Wait for DLT event (after retries exhausted)
        BookingCreatedEvent dltEvent = KafkaTestUtils.waitForBookingCreatedEventInDLT(reservationId);

        // 4️⃣ Validate DLT payload
        assertNotNull(dltEvent, "Message should land in DLT");
        assertEquals("SHOW_1", dltEvent.getShowId());

        // 5️⃣ Business assertions
        assertNull(getBookingStatus(reservationId));
        assertSeatAvailable("SHOW_1", "B10");

        // 6️⃣ Disable timeout simulation (cleanup)
        RestAssured.given()
                .post("http://localhost:9292/internal/test/seat-inventory/timeout/disable")
                .then()
                .statusCode(200);
    }

    @Test
    public void nonRetryableException_shouldGoDirectlyToDLT() throws Exception {
        // 🔹 Arrange
        String bookingId = UUID.randomUUID().toString();
        String userId = UUID.randomUUID().toString();

        BookingCreatedEvent event = new BookingCreatedEvent(
                bookingId,
                userId,
                "SHOW_1",
                List.of("B10"),
                500
        );

        // 🔹 Produce event to main topic
        KafkaTestUtils.publishEvent(
                KafkaConfigProperties.MOVIE_BOOKING_EVENTS_TOPIC,
                bookingId,
                event
        );

        // 🔹 Act: wait for message in DLT
        BookingCreatedEvent dltEvent = KafkaTestUtils.waitForBookingCreatedEventInDLT(bookingId);

        // 🔹 Assert DLT arrival
        assertNotNull(dltEvent, "Message should be published to DLT");
        assertEquals(bookingId, dltEvent.getBookingId());

        // 🔹 Assert original topic header
        ConsumerRecord<String, String> dltRecordWithHeaders =
                BaseKafkaIntegrationTest.createConsumer(
                                "dlt-test-group",
                                String.class,
                                "movie-booking-events-dlt"
                        ).poll(Duration.ofSeconds(5))
                        .iterator().next(); // Get the record just for headers

        Header originalTopicHeader = dltRecordWithHeaders.headers().lastHeader(KafkaHeaders.DLT_ORIGINAL_TOPIC);
        assertNotNull(originalTopicHeader);
        assertEquals(
                KafkaConfigProperties.MOVIE_BOOKING_EVENTS_TOPIC,
                new String(originalTopicHeader.value(), StandardCharsets.UTF_8)
        );

        // 🔹 Assert exception class header
        Header exceptionClassHeader = dltRecordWithHeaders.headers().lastHeader(KafkaHeaders.DLT_EXCEPTION_FQCN);
        assertNotNull(exceptionClassHeader);
        assertEquals(
                NonRecoverableBusinessException.class.getName(),
                new String(exceptionClassHeader.value(), StandardCharsets.UTF_8)
        );

        // 🔹 Assert exception message
        Header exceptionMessageHeader = dltRecordWithHeaders.headers().lastHeader(KafkaHeaders.DLT_EXCEPTION_MESSAGE);
        assertNotNull(exceptionMessageHeader);
        assertTrue(
                new String(exceptionMessageHeader.value(), StandardCharsets.UTF_8)
                        .contains("Seat already locked")
        );
    }


    @AfterClass
    void tearDown() throws SQLException {
        TestDataCleaner.releaseAllSeatsForShow("SHOW_1");
    }
}

