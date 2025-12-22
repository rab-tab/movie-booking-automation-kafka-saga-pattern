package com.microservices.api.util;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class TestDataCleaner {

    /**
     * Deletes booking + booking seats for given bookingCode
     */
    public static void cleanBooking(String bookingCode) throws SQLException {

        try (Connection con = DbTestUtils.getConnection()) {

            // 1️⃣ Get booking id
            Long bookingId = null;
            try (PreparedStatement ps =
                         con.prepareStatement("SELECT id FROM Booking WHERE bookingCode = ?")) {
                ps.setString(1, bookingCode);
                var rs = ps.executeQuery();
                if (rs.next()) bookingId = rs.getLong("id");
            }

            if (bookingId == null) return;

            // 2️⃣ Delete booking_seatIds
            try (PreparedStatement ps =
                         con.prepareStatement("DELETE FROM Booking_seatIds WHERE Booking_id = ?")) {
                ps.setLong(1, bookingId);
                ps.executeUpdate();
            }

            // 3️⃣ Delete booking
            try (PreparedStatement ps =
                         con.prepareStatement("DELETE FROM Booking WHERE id = ?")) {
                ps.setLong(1, bookingId);
                ps.executeUpdate();
            }
        }
    }

    /**
     * Resets seats back to AVAILABLE
     */
    public static void resetSeats(String showId, List<String> seats) throws SQLException {
        String sql =
                "UPDATE seat_inventory " +
                        "SET status = 'AVAILABLE', " +
                        "currentBookingId = NULL " +
                        "WHERE showId = ? " +
                        "AND seatNumber = ?";

        try (Connection con = DbTestUtils.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            for (String seat : seats) {
                ps.setString(1, showId);
                ps.setString(2, seat);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    /**
     * Convenience cleanup for saga tests
     */
    public static void cleanSagaData(
            String bookingCode,
            String showId,
            List<String> seats
    ) throws SQLException {

        cleanBooking(bookingCode);
        resetSeats(showId, seats);
    }

    public static void releaseSeats(String showId, Iterable<String> seats) throws SQLException {
        String sql =
                "UPDATE seat_inventory " +
                        "SET status = 'AVAILABLE', " +
                        "    currentBookingId = NULL " +
                        "WHERE showId = ? " +
                        "  AND seatNumber = ?";

        try (Connection con = DbTestUtils.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            for (String seat : seats) {
                ps.setString(1, showId);
                ps.setString(2, seat);
                ps.addBatch();
            }

            ps.executeBatch();
        }
    }

    public static void releaseAllSeatsForShow(String showId) throws SQLException {
        executeUpdate(
                "UPDATE seat_inventory SET status='AVAILABLE', currentBookingId=NULL WHERE showId=?",
                showId
        );
    }

    public static int executeUpdate(String sql, Object... params) throws SQLException {
        try (Connection conn = DbTestUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }

            return ps.executeUpdate();
        }
    }
}

