package com.konrad.library.lending.service;

import java.time.Instant;

/** Shaped to match the Reservation schema in api/openapi.yaml. */
public record ReservationView(
        Long id,
        Long bookId,
        String bookTitle,
        Long memberId,
        String memberName,
        Instant reservedAt,
        String status,
        Integer queuePosition) {}
