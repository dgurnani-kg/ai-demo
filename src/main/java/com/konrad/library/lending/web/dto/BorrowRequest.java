package com.konrad.library.lending.web.dto;

import jakarta.validation.constraints.NotNull;

/** Wire shape for BorrowRequest in api/openapi.yaml. */
public record BorrowRequest(@NotNull Long bookId, @NotNull Long memberId) {}
