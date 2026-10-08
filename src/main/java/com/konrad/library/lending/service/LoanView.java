package com.konrad.library.lending.service;

import java.time.Instant;

/** Shaped to match the Loan schema in api/openapi.yaml. */
public record LoanView(
        Long id,
        Long bookId,
        String bookTitle,
        Long memberId,
        String memberName,
        Instant borrowedAt,
        Instant dueAt,
        Instant returnedAt) {}
