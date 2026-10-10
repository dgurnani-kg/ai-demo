package com.konrad.library.lending.web.dto;

import jakarta.validation.constraints.NotNull;

/** Wire shape for ReservationRequest in api/openapi.yaml. */
public record ReservationRequest(@NotNull Long memberId) {}
