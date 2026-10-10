package com.konrad.library.lending.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "reservation")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "book_id", nullable = false)
    private Long bookId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private ReservationStatus status;

    @Column(name = "reserved_at", nullable = false)
    private Instant reservedAt;

    @Column(name = "ready_at")
    private Instant readyAt;

    protected Reservation() {
        // for JPA
    }

    public Reservation(Long bookId, Long memberId, Instant reservedAt) {
        this.bookId = bookId;
        this.memberId = memberId;
        this.reservedAt = reservedAt;
        this.status = ReservationStatus.WAITING;
    }

    /** A copy has come back and is now being held for this member. */
    public void markReady(Instant when) {
        this.status = ReservationStatus.READY;
        this.readyAt = when;
    }

    /** The member has borrowed the copy that was held for them. */
    public void markCollected() {
        this.status = ReservationStatus.COLLECTED;
    }

    /** The member gives up their place in the queue. */
    public void cancel() {
        this.status = ReservationStatus.CANCELLED;
    }

    /** True while the reservation still occupies a place in the queue. */
    public boolean isActive() {
        return status == ReservationStatus.WAITING || status == ReservationStatus.READY;
    }

    public Long getId() {
        return id;
    }

    public Long getBookId() {
        return bookId;
    }

    public Long getMemberId() {
        return memberId;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public Instant getReservedAt() {
        return reservedAt;
    }

    public Instant getReadyAt() {
        return readyAt;
    }
}
