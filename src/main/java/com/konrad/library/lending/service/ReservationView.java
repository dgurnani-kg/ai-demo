package com.konrad.library.lending.service;

import java.time.Instant;

/**
 * Shaped to match the Reservation schema in api/openapi.yaml.
 *
 * <p>{@code status} is a String rather than the domain enum on purpose: the web layer may not
 * depend on {@code ..domain..}, so a domain enum cannot appear in a wire DTO.
 */
public record ReservationView(
        Long id,
        Long bookId,
        String bookTitle,
        Long memberId,
        String memberName,
        String status,
        Instant reservedAt,
        Instant readyAt) {}
