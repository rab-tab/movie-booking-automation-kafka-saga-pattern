package facade.impl;

import com.microservices.api.constants.KafkaTestConstants;
import com.microservices.api.model.events.BookingCreatedEvent;
import com.microservices.api.model.events.BookingPaymentEvent;
import com.microservices.api.model.events.SeatReservedEvent;
import com.microservices.api.tests.base.BaseKafkaIntegrationTest;
import facade.BookingSagaKafkaFacade;
import org.apache.kafka.clients.consumer.Consumer;

import java.time.Duration;

public class BookingSagaKafkaFacadeImpl implements BookingSagaKafkaFacade {
    private final Consumer<String, BookingCreatedEvent> bookingConsumer;
    private final Consumer<String, BookingPaymentEvent> paymentConsumer;
    private final Consumer<String, SeatReservedEvent> seatConsumer;

    public BookingSagaKafkaFacadeImpl(
            Consumer<String, BookingCreatedEvent> bookingConsumer,
            Consumer<String, BookingPaymentEvent> paymentConsumer,
            Consumer<String, SeatReservedEvent> seatConsumer
    ) {
        this.bookingConsumer = bookingConsumer;
        this.paymentConsumer = paymentConsumer;
        this.seatConsumer = seatConsumer;
    }


    @Override
    public BookingCreatedEvent awaitBookingCreated() {
        return BaseKafkaIntegrationTest.pollSingleRecord(bookingConsumer, KafkaTestConstants.BOOKING_EVENTS_TOPIC, Duration.ofSeconds(10));
    }

    @Override
    public BookingPaymentEvent awaitPaymentCompleted() {
        return BaseKafkaIntegrationTest.pollSingleRecord(paymentConsumer, KafkaTestConstants.PAYMENT_EVENTS_TOPIC, Duration.ofSeconds(10));

    }

    @Override
    public SeatReservedEvent awaitSeatsReserved() {
        return BaseKafkaIntegrationTest.pollSingleRecord(seatConsumer, KafkaTestConstants.SEAT_RESERVED_TOPIC, Duration.ofSeconds(10));

    }

    @Override
    public void close() throws Exception {
        bookingConsumer.close();
        paymentConsumer.close();
        seatConsumer.close();
    }
}
