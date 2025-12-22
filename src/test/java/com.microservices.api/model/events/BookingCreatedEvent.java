package com.microservices.api.model.events;


import java.util.List;

public class BookingCreatedEvent {

    private String bookingId;
    private String userId;
    private String showId;
    private List<String> seatIds;
    private long amount;

    // Default constructor (needed for deserialization)
    public BookingCreatedEvent() {
    }

    // All-args constructor
    public BookingCreatedEvent(String bookingId, String userId, String showId, List<String> seatIds, long amount) {
        this.bookingId = bookingId;
        this.userId = userId;
        this.showId = showId;
        this.seatIds = seatIds;
        this.amount = amount;
    }

    // Getters and setters
    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getShowId() {
        return showId;
    }

    public void setShowId(String showId) {
        this.showId = showId;
    }

    public List<String> getSeatIds() {
        return seatIds;
    }

    public void setSeatIds(List<String> seatIds) {
        this.seatIds = seatIds;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    @Override
    public String toString() {
        return "BookingCreatedEvent{" +
                "bookingId='" + bookingId + '\'' +
                ", userId='" + userId + '\'' +
                ", showId='" + showId + '\'' +
                ", seatIds=" + seatIds +
                ", amount=" + amount +
                '}';
    }
}
