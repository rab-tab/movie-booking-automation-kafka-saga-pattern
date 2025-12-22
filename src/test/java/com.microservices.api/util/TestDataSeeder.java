package com.microservices.api.util;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

public class TestDataSeeder {

    public static void seedSeatInventory(String showId, String theaterId, String screenId, List<String> seatNumbers) throws SQLException
    {
        String sql =
                "INSERT INTO seat_inventory " +
                        "(showId, theaterId, screenId, seatNumber, status, lastUpdated) " +
                        "VALUES (?, ?, ?, ?, 'AVAILABLE', ?) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "status = 'AVAILABLE', " +
                        "currentBookingId = NULL, " +
                        "lastUpdated = VALUES(lastUpdated)";
        try (Connection con = DbTestUtils.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            for (String seat : seatNumbers) {
                ps.setString(1, showId);
                ps.setString(2, theaterId);
                ps.setString(3, screenId);
                ps.setString(4, seat);
                ps.setObject(5, Instant.now());
                ps.addBatch();
            }

            ps.executeBatch();
        }
    }
    public static void lockSeat(
            String showId,
            String seatNumber,
            String bookingId
    ) throws SQLException {

        String sql =
                "UPDATE seat_inventory " +
                        "SET status = 'LOCKED', " +
                        "    currentBookingId = ?, " +
                        "    lastUpdated = ? " +
                        "WHERE showId = ? " +
                        "  AND seatNumber = ?";

        try (Connection con = DbTestUtils.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, bookingId);
            ps.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            ps.setString(3, showId);
            ps.setString(4, seatNumber);

            ps.executeUpdate();
        }
    }
}

