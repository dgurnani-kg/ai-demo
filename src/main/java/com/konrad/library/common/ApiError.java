package com.konrad.library.common;

/** The single error shape the API returns. Mirrors ApiError in api/openapi.yaml. */
public record ApiError(int status, String error, String message) {}
