package com.konrad.library.lending.domain;

/** Shaped to match the ReservationStatus schema in api/openapi.yaml. */
public enum ReservationStatus {
    /** In the queue, no copy held yet. */
    WAITING,
    /** A copy has come back and is held for this member. */
    READY,
    /** The member borrowed the copy that was held. */
    COLLECTED,
    /** The member gave up their place. */
    CANCELLED
}
