package facade;

import com.microservices.api.model.events.BookingCreatedEvent;
import com.microservices.api.model.events.BookingPaymentEvent;
import com.microservices.api.model.events.SeatReservedEvent;

public interface BookingSagaKafkaFacade extends AutoCloseable {
    BookingCreatedEvent awaitBookingCreated();

    BookingPaymentEvent awaitPaymentCompleted();

    SeatReservedEvent awaitSeatsReserved();
}
