package com.microservices.api.tests.failures.saga;

import com.microservices.api.model.events.BookingPaymentEvent;
import com.microservices.api.model.events.SeatReservedEvent;
import com.microservices.api.model.request.BookingRequest;
import com.microservices.api.model.response.BookingResponse;
import com.microservices.api.tests.BaseKafkaIntegrationTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.apache.kafka.clients.consumer.Consumer;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import com.microservices.api.util.DBHelper;
import com.microservices.api.util.KafkaTestUtils;

import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.testng.Assert.assertFalse;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNull;
import static com.microservices.api.util.DBHelper.assertBookingStatus;


public class BookingPaymentFailureSagaTest extends BaseKafkaIntegrationTest {
    private Consumer<String, BookingPaymentEvent> paymentConsumer;
    private Consumer<String, SeatReservedEvent> seatConsumer;

    private static final String PAYMENT_TOPIC = "payment-events";
    private static final String SEAT_TOPIC = "seat-reserved-topic";

    private static String reservationId;
    private static String bookingStatus;
    private static UUID userId;
    private static List<String> seats;

    @BeforeClass
    void setup() throws Exception {
      /*  seats = List.of("S1", "S2");
        try {
            TestDataSeeder.seedSeatInventory(
                    "SHOW_1",
                    "THEATER_1",
                    "SCREEN_1",
                    seats
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }*/

        paymentConsumer = createConsumer("payment-failure-it-group", BookingPaymentEvent.class, PAYMENT_TOPIC);
        seatConsumer = createConsumer("seat-release-it-group", SeatReservedEvent.class, SEAT_TOPIC);
        // 1️⃣ Clear Kafka topics before test
        KafkaTestUtils.deleteTopicRecords("localhost:9092", PAYMENT_TOPIC);
        KafkaTestUtils.deleteTopicRecords("localhost:9092", SEAT_TOPIC);
        //seatConsumer.seekToEnd(seatConsumer.assignment());

        //paymentConsumer.seekToEnd(paymentConsumer.assignment());
    }

    @Test
    public void payment_failure_should_release_seats_and_update_db() throws SQLException, InterruptedException {
        userId = UUID.randomUUID();
        seats = List.of("A1", "A2");

        // Set amount > 3000 to trigger payment failure
        BookingRequest request = new BookingRequest("BKG_FAIL_1", "SHOW_1", seats, String.valueOf(userId), Instant.now(), 5000);
        Response response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(request).log().all()
                .post("http://localhost:9191/booking-service/bookSeat")
                .then().log().all()
                .statusCode(200)
                .extract().response();

        BookingResponse bookingResponse = response.as(BookingResponse.class);
        reservationId = bookingResponse.getReservationId();
        System.out.println("reservation id "+reservationId);

       /* Map<String, Object> booking =
                DBHelper.fetchBookingByCode(reservationId);

        System.out.println("DB amount = " + booking.get("amount"));*/

        // 1️⃣ Payment FAILED event
       // BookingPaymentEvent paymentEvent = pollSingleRecord(paymentConsumer, PAYMENT_TOPIC, Duration.ofSeconds(5));
        BookingPaymentEvent paymentEvent =
                pollForBookingEvent(
                        paymentConsumer,
                        PAYMENT_TOPIC,
                        Duration.ofSeconds(15),
                        reservationId,
                        BookingPaymentEvent::getBookingId
                );
        System.out.println("Payment Event = " + paymentEvent);
        assertEquals(false,paymentEvent.isPaymentCompleted());

        // 2️⃣ Seat RELEASED event
        //SeatReservedEvent seatEvent = pollSingleRecord(seatConsumer, SEAT_TOPIC, Duration.ofSeconds(5));
        SeatReservedEvent seatEvent =
                pollForBookingEvent(
                        seatConsumer,
                        SEAT_TOPIC,
                        Duration.ofSeconds(15),
                        reservationId,
                        SeatReservedEvent::getBookingId
                );

        System.out.println("SeatReleased Event = " + seatEvent);
        //assertFalse(seatEvent.isReserved());
        //assertEquals(false,seatEvent.isReserved());

        // 3️⃣ DB consistency
        assertBookingStatus(reservationId, "FAILED");
        Map<String, Map<String, String>> seatStates = DBHelper.fetchSeatStates("SHOW_1", seats);
        assertEquals(
                "Seat count mismatch",
                seats.size(),
                seatStates.size()
        );

        for (String seat : seats) {
            Map<String, String> state = seatStates.get(seat);

            assertEquals("Seat not AVAILABLE: " + seat, "AVAILABLE", state.get("status"));
            assertNull("Seat still locked for booking: " + seat, state.get("currentBookingId"));
        }
    }

    @AfterClass
    void tearDown() {
          /*  try {
                TestDataCleaner.cleanSagaData(
                        "BKG_FAIL_1",
                        "SHOW_1",
                        seats
                );
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }*/
        paymentConsumer.close();
        seatConsumer.close();
    }

}



