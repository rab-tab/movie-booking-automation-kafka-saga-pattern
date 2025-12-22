package com.microservices.api.model.events;

public class SeatReservedEvent {
    private String bookingId;
    private boolean reserved;
    private long amount;

    public SeatReservedEvent() {}

    public SeatReservedEvent(String bookingId, boolean reserved, long amount) {
        this.bookingId = bookingId;
        this.reserved = reserved;
        this.amount = amount;
    }

    // getters and setters
    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public boolean isReserved() { return reserved; }
    public void setReserved(boolean reserved) { this.reserved = reserved; }

    public long getAmount() { return amount; }
    public void setAmount(long amount) { this.amount = amount; }

    @Override
    public String toString() {
        return "SeatReservedEvent{" +
                "bookingId='" + bookingId + '\'' +
                ", reserved=" + reserved +
                ", amount=" + amount +
                '}';
    }
}
