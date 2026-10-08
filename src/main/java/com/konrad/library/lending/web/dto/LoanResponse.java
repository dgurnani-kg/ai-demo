package com.konrad.library.lending.web.dto;

import com.konrad.library.lending.service.LoanView;
import java.time.Instant;

/** Wire shape for Loan. Must stay identical to the Loan schema in api/openapi.yaml. */
public record LoanResponse(
        Long id,
        Long bookId,
        String bookTitle,
        Long memberId,
        String memberName,
        Instant borrowedAt,
        Instant dueAt,
        Instant returnedAt) {

    public static LoanResponse from(LoanView view) {
        return new LoanResponse(
                view.id(),
                view.bookId(),
                view.bookTitle(),
                view.memberId(),
                view.memberName(),
                view.borrowedAt(),
                view.dueAt(),
                view.returnedAt());
    }
}
