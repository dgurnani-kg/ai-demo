package com.konrad.library.lending.web.dto;

import com.konrad.library.lending.service.ReservationView;
import java.time.Instant;

/** Wire shape for Reservation. Must stay identical to the Reservation schema in api/openapi.yaml. */
public record ReservationResponse(
        Long id,
        Long bookId,
        String bookTitle,
        Long memberId,
        String memberName,
        Instant reservedAt,
        String status,
        Integer queuePosition) {

    public static ReservationResponse from(ReservationView view) {
        return new ReservationResponse(
                view.id(),
                view.bookId(),
                view.bookTitle(),
                view.memberId(),
                view.memberName(),
                view.reservedAt(),
                view.status(),
                view.queuePosition());
    }
}
