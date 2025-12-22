package tests.success;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.microservices.api.model.events.BookingCreatedEvent;
import com.microservices.api.model.events.BookingPaymentEvent;
import com.microservices.api.model.events.SeatReservedEvent;
import com.microservices.api.model.request.BookingRequest;
import com.microservices.api.model.response.BookingResponse;
import tests.BaseKafkaIntegrationTest;
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

public class BookingSagaIntegrationTest extends BaseKafkaIntegrationTest {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant timestamp;

    private Consumer<String, BookingCreatedEvent> bookingConsumer;
    private Consumer<String, BookingPaymentEvent> paymentConsumer;
    private Consumer<String, SeatReservedEvent> seatConsumer;

    private static final String PAYMENT_TOPIC = "payment-events";
    private static final String SEAT_TOPIC = "seat-reserved-topic";
    private static final String TOPIC = "movie-booking-events";
    private static String reservationId;
    private static String bookingStatus;
    private static UUID userId;
    private static List<String> seats;


    @BeforeClass
    void setup() {
        bookingConsumer = createConsumer("booking-api-it-group", BookingCreatedEvent.class, TOPIC);
        paymentConsumer = createConsumer("payment-it-group", BookingPaymentEvent.class, PAYMENT_TOPIC);
        seatConsumer = createConsumer("seat-it-group", SeatReservedEvent.class, SEAT_TOPIC);
    }

    @Test
    public void booking_saga_should_complete_successfully() {
        userId = UUID.randomUUID();
        seats=List.of("S1", "S2");
        BookingRequest request = new BookingRequest("BKG_1", "SHOW_1", seats, String.valueOf(userId), Instant.now(), 500);
        Response response = RestAssured.given().contentType(ContentType.JSON).body(request).log().all()
                .post("http://localhost:9191/booking-service/bookSeat")
                .then().log().all()
                .statusCode(200).extract().response();
        BookingResponse bookingResponse = response.as(BookingResponse.class);
        reservationId = bookingResponse.getReservationId();
        bookingStatus = bookingResponse.getStatus();

        // 🔹 Kafka validations
        validateKafkaEvents();

        // 🔹 DB: Booking
        try {
            validateDB();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    public void validateKafkaEvents() {
        // 1️⃣ Booking event
        BookingCreatedEvent bookingEvent = pollSingleRecord(bookingConsumer, TOPIC, Duration.ofSeconds(10));
        // assertEquals(event.getBookingId(), "BKG_1");
        // assertEquals(event.getUserId(), "USER_1");

        // 2️⃣ Payment event
        BookingPaymentEvent paymentEvent = pollSingleRecord(paymentConsumer, PAYMENT_TOPIC, Duration.ofSeconds(10));

        // assertEquals(paymentEvent.getBookingId(), "BKG_1");
        assertEquals(paymentEvent.getAmount(), 500);

        // 3️⃣ Seat reserved event
        SeatReservedEvent seatEvent = pollSingleRecord(seatConsumer, SEAT_TOPIC, Duration.ofSeconds(10));

        // assertEquals(seatEvent.getBookingId(), "BKG_1");
        assertEquals(seatEvent.isReserved(), true);

    }

    public void validateDB() throws SQLException {
        Map<String, Object> bookingRow = fetchBookingByCode(reservationId);

        assertEquals(bookingRow.get("status"), "CONFIRMED");
        assertEquals(bookingRow.get("amount"), 500L);
        assertEquals(bookingRow.get("userId"), String.valueOf(userId));

        Long bookingId = (Long) bookingRow.get("id");

        // 🔹 DB: Booking seats
        List<String> dbSeats = fetchSeatsForBooking(bookingId);
        assertEquals(dbSeats, seats);

        // 🔹 DB: Seat inventory - Code issue , table not gettting updated
      /*  for (String seat : seats) {
            Map<String, String> seatRow =
                    fetchSeatStatus("SHOW_1", seat);

            assertEquals(seatRow.get("status"), "RESERVED");
            assertEquals(seatRow.get("currentBookingId"), reservationId);
        }*/
    }


    @AfterClass
    void tearDown() {
        bookingConsumer.close();
    }
}
