package com.microservices.api.tests.success;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.microservices.api.constants.KafkaTestConstants;
import com.microservices.api.model.events.BookingCreatedEvent;
import com.microservices.api.model.events.BookingPaymentEvent;
import com.microservices.api.model.events.SeatReservedEvent;
import com.microservices.api.model.request.BookingRequest;
import com.microservices.api.model.response.BookingResponse;
import com.microservices.api.tests.base.BaseKafkaIntegrationTest;
import facade.BookingSagaDbFacade;
import facade.BookingSagaKafkaFacade;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.apache.kafka.clients.consumer.Consumer;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static com.microservices.api.util.DBHelper.*;
import static org.testng.AssertJUnit.assertTrue;

public class BookingSagaIntegrationTest extends BaseKafkaIntegrationTest {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant timestamp;

    private static String reservationId;
    private static String bookingStatus;
    private static UUID userId;
    private static List<String> seats;
    BookingSagaKafkaFacade bookingSagaKafkaFacade;
    BookingSagaDbFacade bookingSagaDbFacade;


    @Test
    public void booking_saga_should_complete_successfully() {
        userId = UUID.randomUUID();
        seats = List.of("S1", "S2");
        BookingRequest request = new BookingRequest("BKG_1", "SHOW_1", seats, String.valueOf(userId), Instant.now(), 500);
        Response response = RestAssured.given().contentType(ContentType.JSON).body(request).log().all()
                .post("http://localhost:9191/booking-service/bookSeat")
                .then().log().all()
                .statusCode(200).extract().response();
        BookingResponse bookingResponse = response.as(BookingResponse.class);
        reservationId = bookingResponse.getReservationId();
        bookingStatus = bookingResponse.getStatus();

        // 🔹 Kafka validations
        BookingCreatedEvent bookingEvent = bookingSagaKafkaFacade.awaitBookingCreated();
        BookingPaymentEvent paymentEvent = bookingSagaKafkaFacade.awaitPaymentCompleted();
        SeatReservedEvent seatEvent = bookingSagaKafkaFacade.awaitSeatsReserved();
        assertTrue(seatEvent.isReserved());

        // 🔹 DB: Booking
        bookingSagaDbFacade.assertBookingConfirmed(reservationId,
                500L, String.valueOf(userId));

        bookingSagaDbFacade.assertSeatsReserved(reservationId, seats, String.valueOf(userId));
    }
}
