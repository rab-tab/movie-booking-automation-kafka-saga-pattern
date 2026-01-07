package com.microservices.api.constants;

public final class KafkaTestConstants {

    private KafkaTestConstants() {}

    // 🔹 Core saga topics
    public static final String BOOKING_EVENTS_TOPIC = "movie-booking-events";
    public static final String PAYMENT_EVENTS_TOPIC = "payment-events";
    public static final String SEAT_RESERVED_TOPIC = "seat-reserved-topic";

    // 🔹 DLT topics (future-safe)
    public static final String BOOKING_EVENTS_DLT = "movie-booking-events-dlt";
    public static final String PAYMENT_EVENTS_DLT = "payment-events-dlt";
}