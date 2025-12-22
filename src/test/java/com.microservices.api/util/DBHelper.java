package com.microservices.api.util;

import java.sql.*;
import java.util.*;

import static org.testng.AssertJUnit.assertEquals;

public class DBHelper {
    public static Map<String, Object> fetchBookingByCode(String bookingCode) throws SQLException {

        System.out.println("In Db helper, bookingCode --- "+ bookingCode);
        String sql = "SELECT * FROM Booking WHERE bookingCode = ?";

        try (Connection con = DbTestUtils.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, bookingCode);

            ResultSet rs = ps.executeQuery();
            rs.next();
            System.out.println("Booking code from DB booking " +rs.getString("bookingCode"));
           // if (!rs.next()) return null;

            Map<String, Object> row = new HashMap<>();
            row.put("id", rs.getLong("id"));
            row.put("status", rs.getString("status"));
            row.put("amount", rs.getLong("amount"));
            row.put("userId", rs.getString("userId"));
            row.put("showId", rs.getString("showId"));
            System.out.println("Map size is "+row.size());
            return row;
        }
    }

    public static List<String> fetchSeatsForBooking(Long bookingId) throws SQLException {

        String sql = "SELECT seatIds FROM Booking_seatIds WHERE Booking_id = ?";

        try (Connection con = DbTestUtils.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, bookingId);

            ResultSet rs = ps.executeQuery();
            List<String> seats = new ArrayList<>();
            while (rs.next()) {
                seats.add(rs.getString("seatIds"));
            }
            return seats;
        }
    }
    public static Map<String, String> fetchSeatStatus(String showId, String seat) throws SQLException {

        String sql =
                "SELECT status, currentBookingId FROM seat_inventory " +
                        "WHERE showId = ? AND seatNumber = ?";

        try (Connection con = DbTestUtils.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, showId);
            ps.setString(2, seat);

            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return null;

            Map<String, String> row = new HashMap<>();
            row.put("status", rs.getString("status"));
            row.put("currentBookingId", rs.getString("currentBookingId"));
            return row;
        }
    }
    // Assert booking status for saga / payment failure
    public static void assertBookingStatus(String bookingCode, String expectedStatus) throws SQLException {
        Map<String, Object> booking = fetchBookingByCode(bookingCode);
        if (booking.size()== 0) {
            throw new AssertionError("Booking not found for code: " + bookingCode);
        }
        String actualStatus = (String) booking.get("status");
        assertEquals(actualStatus.trim().toUpperCase(), expectedStatus.trim().toUpperCase());
    }

    public static Map<String, Map<String, String>> fetchSeatStates(String showId, List<String> seats) throws SQLException {

        String placeholders = String.join(
                ",",
                Collections.nCopies(seats.size(), "?")
        );

        String sql =
                "SELECT seatNumber, status, currentBookingId " +
                        "FROM seat_inventory " +
                        "WHERE showId = ? " +
                        "AND seatNumber IN (" + placeholders + ")";

        Map<String, Map<String, String>> result = new HashMap<>();

        try (Connection conn = DbTestUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, showId);
            for (int i = 0; i < seats.size(); i++) {
                ps.setString(i + 2, seats.get(i));
            }

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Map<String, String> seatState = new HashMap<>();
                seatState.put("status", rs.getString("status"));
                seatState.put("currentBookingId", rs.getString("currentBookingId"));

                result.put(rs.getString("seatNumber"), seatState);
            }
        }
        return result;
    }

    public static void assertSeatLocked(String showId, String seatNumber, String expectedBookingId) throws SQLException {

        Map<String, Object> seat =
                fetchSingleRow(
                        "SELECT status, currentBookingId FROM seat_inventory WHERE showId=? AND seatNumber=?",
                        showId,
                        seatNumber
                );

        assertEquals(String.valueOf(seat.get("status")), "LOCKED");
       // assertEquals(seat.get("currentBookingId"), expectedBookingId);
    }

    public static void assertSeatAvailable(String showId, String seatNumber) throws SQLException {
        Map<String, Object> row =
                fetchSingleRow(
                        "SELECT status, currentBookingId FROM seat_inventory WHERE showId=? AND seatNumber=?",
                        showId,
                        seatNumber
                );

        assertEquals(String.valueOf(row.get("status")), "AVAILABLE");
    }

    public static Map<String, Object> fetchSingleRow(
            String sql,
            Object... params
    ) throws SQLException {

        try (Connection conn = DbTestUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }

            try (ResultSet rs = ps.executeQuery()) {

                if (!rs.next()) {
                    throw new AssertionError("Expected 1 row but found 0");
                }

                ResultSetMetaData meta = rs.getMetaData();
                int columnCount = meta.getColumnCount();

                Map<String, Object> row = new HashMap<>();

                for (int i = 1; i <= columnCount; i++) {
                    String columnName = meta.getColumnLabel(i);
                    Object value = rs.getObject(i);
                    row.put(columnName, value);
                }

                if (rs.next()) {
                    throw new AssertionError("Expected 1 row but found more than 1");
                }

                return row;
            }
        }
    }


}
