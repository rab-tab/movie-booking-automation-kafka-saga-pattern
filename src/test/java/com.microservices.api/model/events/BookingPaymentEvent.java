package com.microservices.api.model.events;

public class BookingPaymentEvent {

    private String bookingId;
    private boolean paymentCompleted;
    private long amount;

    // Default constructor (needed for deserialization)
    public BookingPaymentEvent() {
    }

    // All-args constructor
    public BookingPaymentEvent(String bookingId, boolean paymentCompleted, long amount) {
        this.bookingId = bookingId;
        this.paymentCompleted = paymentCompleted;
        this.amount = amount;
    }

    // Getters and setters
    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public boolean isPaymentCompleted() {
        return paymentCompleted;
    }

    public void setPaymentCompleted(boolean paymentCompleted) {
        this.paymentCompleted = paymentCompleted;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    @Override
    public String toString() {
        return "BookingPaymentEvent{" +
                "bookingId='" + bookingId + '\'' +
                ", paymentCompleted=" + paymentCompleted +
                ", amount=" + amount +
                '}';
    }
}
