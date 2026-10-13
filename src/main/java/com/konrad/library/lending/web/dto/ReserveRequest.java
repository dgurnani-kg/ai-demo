package com.konrad.library.lending.web.dto;

import jakarta.validation.constraints.NotNull;

/** Wire shape for ReserveRequest in api/openapi.yaml. */
public record ReserveRequest(@NotNull Long memberId) {}
