package com.konrad.library.lending.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

    @Column(name = "reserved_at", nullable = false)
    private Instant reservedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    protected Reservation() {
        // for JPA
    }

    public Reservation(Long bookId, Long memberId, Instant reservedAt) {
        this.bookId = bookId;
        this.memberId = memberId;
        this.reservedAt = reservedAt;
    }

    public void cancelOn(Instant when) {
        this.cancelledAt = when;
    }

    public boolean isActive() {
        return cancelledAt == null;
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

    public Instant getReservedAt() {
        return reservedAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }
}
