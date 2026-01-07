package com.microservices.api.util;

import com.microservices.api.model.events.BookingCreatedEvent;

public interface SeatInventoryService {
    void handleBooking(BookingCreatedEvent event);
}
