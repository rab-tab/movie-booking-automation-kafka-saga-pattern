package com.microservices.api.model.request;


import lombok.*;

import java.time.Instant;
import java.util.List;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookingRequest {

    private String reservationId;
    private String showId;
    private List<String> seatIds;
    private String userId;
    private Instant timestamp;
    private long amount;



    // Getters and Setters
    public String getReservationId() {
        return reservationId;
    }

    public void setReservationId(String reservationId) {
        this.reservationId = reservationId;
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

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    @Override
    public String toString() {
        return "BookingRequest{" +
                "reservationId='" + reservationId + '\'' +
                ", showId='" + showId + '\'' +
                ", seatIds=" + seatIds +
                ", userId='" + userId + '\'' +
                ", timestamp=" + timestamp +
                ", amount=" + amount +
                '}';
    }
}

