package com.konrad.library.lending.service;

import com.konrad.library.catalog.service.BookService;
import com.konrad.library.catalog.service.BookView;
import com.konrad.library.common.ConflictException;
import com.konrad.library.common.NotFoundException;
import com.konrad.library.lending.domain.LoanRepository;
import com.konrad.library.lending.domain.Reservation;
import com.konrad.library.lending.domain.ReservationRepository;
import com.konrad.library.lending.domain.ReservationStatus;
import com.konrad.library.membership.service.MemberService;
import com.konrad.library.membership.service.MemberView;
import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The waiting queue for titles that are fully out on loan.
 *
 * <p>The central idea is a <em>free</em> copy: a copy on the shelf that is not being held for a
 * member whose reservation has turned READY. Only free copies can be borrowed off the street.
 *
 * <p>This service knows nothing about {@link LoanService}; the dependency runs the other way.
 */
@Service
@Transactional(readOnly = true)
public class ReservationService {

    private static final List<ReservationStatus> ACTIVE_STATUSES =
            List.of(ReservationStatus.WAITING, ReservationStatus.READY);

    private final ReservationRepository reservations;
    private final LoanRepository loans;
    private final BookService books;
    private final MemberService members;
    private final Clock clock;

    public ReservationService(
            ReservationRepository reservations,
            LoanRepository loans,
            BookService books,
            MemberService members,
            Clock clock) {
        this.reservations = reservations;
        this.loans = loans;
        this.books = books;
        this.members = members;
        this.clock = clock;
    }

    /**
     * Reservations newest first. A null {@code memberId} means every member; a null or blank
     * {@code status} means every status. An unrecognised status simply matches nothing.
     */
    public List<ReservationView> list(Long memberId, String status) {
        if (status == null || status.isBlank()) {
            List<Reservation> found = memberId == null
                    ? reservations.findAllByOrderByReservedAtDesc()
                    : reservations.findByMemberIdOrderByReservedAtDesc(memberId);
            return found.stream().map(this::toView).toList();
        }
        Optional<ReservationStatus> parsed = parseStatus(status);
        if (parsed.isEmpty()) {
            return List.of();
        }
        List<Reservation> found = memberId == null
                ? reservations.findByStatusOrderByReservedAtDesc(parsed.get())
                : reservations.findByMemberIdAndStatusOrderByReservedAtDesc(memberId, parsed.get());
        return found.stream().map(this::toView).toList();
    }

    /** Join the queue for a title that has nothing free on the shelf. */
    @Transactional
    public ReservationView reserve(long bookId, long memberId) {
        BookView book = books.get(bookId);
        members.get(memberId);

        if (freeCopies(book) > 0) {
            throw new ConflictException("There are copies of \"" + book.title() + "\" on the shelf; borrow it instead");
        }
        boolean alreadyHasIt = loans.findByMemberIdOrderByBorrowedAtDesc(memberId).stream()
                .anyMatch(loan -> loan.isOpen() && loan.getBookId().equals(bookId));
        if (alreadyHasIt) {
            throw new ConflictException("That member already has this book on loan");
        }
        if (reservations.existsByBookIdAndMemberIdAndStatusIn(bookId, memberId, ACTIVE_STATUSES)) {
            throw new ConflictException("That member is already in the queue for this book");
        }

        Reservation saved = reservations.save(new Reservation(bookId, memberId, clock.instant()));
        return toView(saved);
    }

    /** Give up a place in the queue. A held copy is handed to the next member waiting. */
    @Transactional
    public ReservationView cancel(long reservationId) {
        Reservation reservation = reservations
                .findById(reservationId)
                .orElseThrow(() -> new NotFoundException("No reservation with id " + reservationId));
        if (!reservation.isActive()) {
            throw new ConflictException("That reservation is no longer active");
        }
        boolean wasHoldingACopy = reservation.getStatus() == ReservationStatus.READY;
        reservation.cancel();
        ReservationView view = toView(reservations.save(reservation));
        if (wasHoldingACopy) {
            promoteNextInQueue(reservation.getBookId());
        }
        return view;
    }

    /** How many copies on the shelf are spoken for by a READY reservation. */
    public int readyHolds(long bookId) {
        return (int) reservations.countByBookIdAndStatus(bookId, ReservationStatus.READY);
    }

    /** True when a copy of this book is currently being held for this member. */
    public boolean holdsReadyCopy(long bookId, long memberId) {
        return reservations
                .findFirstByBookIdAndMemberIdAndStatusOrderByReservedAtAsc(bookId, memberId, ReservationStatus.READY)
                .isPresent();
    }

    /** The member has borrowed the copy that was held for them. No-op if nothing was held. */
    @Transactional
    public void collect(long bookId, long memberId) {
        reservations
                .findFirstByBookIdAndMemberIdAndStatusOrderByReservedAtAsc(bookId, memberId, ReservationStatus.READY)
                .ifPresent(reservation -> {
                    reservation.markCollected();
                    reservations.save(reservation);
                });
    }

    /**
     * Hand every free copy of this book to the members who have waited longest. A loop rather
     * than a single step, so it stays correct when several copies come back at once.
     */
    @Transactional
    public void promoteNextInQueue(long bookId) {
        while (freeCopies(books.get(bookId)) > 0) {
            Optional<Reservation> next =
                    reservations.findFirstByBookIdAndStatusOrderByReservedAtAsc(bookId, ReservationStatus.WAITING);
            if (next.isEmpty()) {
                return;
            }
            Reservation reservation = next.get();
            reservation.markReady(clock.instant());
            reservations.save(reservation);
        }
    }

    private int freeCopies(BookView book) {
        return book.availableCopies() - readyHolds(book.id());
    }

    private Optional<ReservationStatus> parseStatus(String status) {
        return Arrays.stream(ReservationStatus.values())
                .filter(candidate -> candidate.name().equalsIgnoreCase(status.trim()))
                .findFirst();
    }

    private ReservationView toView(Reservation reservation) {
        BookView book = books.get(reservation.getBookId());
        MemberView member = members.get(reservation.getMemberId());
        return new ReservationView(
                reservation.getId(),
                reservation.getBookId(),
                book.title(),
                reservation.getMemberId(),
                member.name(),
                reservation.getStatus().name(),
                reservation.getReservedAt(),
                reservation.getReadyAt());
    }
}
