package com.konrad.library.lending.web;

import com.konrad.library.lending.service.ReservationService;
import com.konrad.library.lending.web.dto.ReservationResponse;
import com.konrad.library.lending.web.dto.ReserveRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReservationController {

    private final ReservationService reservations;

    public ReservationController(ReservationService reservations) {
        this.reservations = reservations;
    }

    @PostMapping("/api/books/{bookId}/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponse reserveBook(@PathVariable long bookId, @Valid @RequestBody ReserveRequest request) {
        return ReservationResponse.from(reservations.reserve(bookId, request.memberId()));
    }

    @GetMapping("/api/reservations")
    public List<ReservationResponse> listReservations(
            @RequestParam(name = "memberId", required = false) Long memberId) {
        return reservations.list(memberId).stream()
                .map(ReservationResponse::from)
                .toList();
    }

    @PostMapping("/api/reservations/{reservationId}/cancel")
    public ReservationResponse cancelReservation(@PathVariable long reservationId) {
        return ReservationResponse.from(reservations.cancel(reservationId));
    }
}
