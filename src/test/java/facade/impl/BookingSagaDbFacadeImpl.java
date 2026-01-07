package facade.impl;

import com.microservices.api.util.DBHelper;
import facade.BookingSagaDbFacade;
import org.testng.Assert;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static com.microservices.api.util.DBHelper.fetchBookingByCode;
import static com.microservices.api.util.DBHelper.fetchSeatsForBooking;
import static org.testng.AssertJUnit.assertEquals;

public class BookingSagaDbFacadeImpl implements BookingSagaDbFacade {
    private DBHelper dbHelper;

    public BookingSagaDbFacadeImpl(DBHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    @Override
    public void assertBookingConfirmed(String reservationId, Long expectedAmount, String expectedUserId) {
        Map<String, Object> bookingRow =
                null;
        try {
            bookingRow = dbHelper.fetchBookingByCode(reservationId);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        assertEquals("CONFIRMED", bookingRow.get("status"));
        assertEquals(expectedAmount, bookingRow.get("amount"));
        assertEquals(expectedUserId, bookingRow.get("userId"));

    }

    @Override
    public void assertSeatsReserved(String reservationId, List<String> expectedSeats,String expectedUserId) {
        Map<String, Object> bookingRow = null;
        try {
            bookingRow = fetchBookingByCode(reservationId);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        Assert.assertEquals(bookingRow.get("status"), "CONFIRMED");
        Assert.assertEquals(bookingRow.get("amount"), 500L);
        Assert.assertEquals(bookingRow.get("userId"), String.valueOf(expectedUserId));

        Long bookingId = (Long) bookingRow.get("id");
        // 🔹 DB: Booking seats
        List<String> dbSeats = null;
        try {
            dbSeats = fetchSeatsForBooking(bookingId);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        Assert.assertEquals(dbSeats, expectedSeats);

    }
}
