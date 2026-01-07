package facade;

import java.util.List;

public interface BookingSagaDbFacade {

    void assertBookingConfirmed(
            String reservationId,
            Long expectedAmount,
            String expectedUserId
    );

    void assertSeatsReserved(
            String reservationId,
            List<String> expectedSeats,
            String expectedUserId
    );
}
