package com.microservices.api.model.response;

import lombok.*;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {

    private String reservationId;
    private String status;

    // Getters and Setters
    public String getReservationId() {
        return reservationId;
    }

    public void setReservationId(String reservationId) {
        this.reservationId = reservationId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "BookingResponse{" +
                "reservationId='" + reservationId + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
