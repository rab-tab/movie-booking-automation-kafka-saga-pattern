package com.microservices.api.util;

import com.microservices.api.model.events.BookingCreatedEvent;

public class SeatInventoryListener {

    private final SeatInventoryService seatInventoryService;

    public SeatInventoryListener(SeatInventoryService seatInventoryService) {
        this.seatInventoryService = seatInventoryService;
    }

    public void consume(BookingCreatedEvent event) {
        seatInventoryService.handleBooking(event);
    }
}
