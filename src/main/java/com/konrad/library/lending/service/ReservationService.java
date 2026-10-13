package com.konrad.library.lending.service;

import com.konrad.library.catalog.service.BookService;
import com.konrad.library.catalog.service.BookView;
import com.konrad.library.common.ConflictException;
import com.konrad.library.common.NotFoundException;
import com.konrad.library.lending.domain.LoanRepository;
import com.konrad.library.lending.domain.Reservation;
import com.konrad.library.lending.domain.ReservationRepository;
import com.konrad.library.membership.service.MemberService;
import com.konrad.library.membership.service.MemberView;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReservationService {

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

    public List<ReservationView> list(Long memberId) {
        List<Reservation> found = memberId == null
                ? reservations.findAllByOrderByReservedAtDesc()
                : reservations.findByMemberIdOrderByReservedAtDesc(memberId);
        return found.stream().map(this::toView).toList();
    }

    @Transactional
    public ReservationView reserve(long bookId, long memberId) {
        BookView book = books.get(bookId);
        members.get(memberId);

        if (book.availableCopies() > 0) {
            throw new ConflictException("Copies of \"" + book.title() + "\" are on the shelf; borrow it instead");
        }

        boolean alreadyOnLoan = loans.findByMemberIdOrderByBorrowedAtDesc(memberId).stream()
                .anyMatch(loan -> loan.isOpen() && loan.getBookId().equals(bookId));
        if (alreadyOnLoan) {
            throw new ConflictException("That member already has this book on loan");
        }

        boolean alreadyReserved = reservations.findByMemberIdOrderByReservedAtDesc(memberId).stream()
                .anyMatch(reservation ->
                        reservation.isActive() && reservation.getBookId().equals(bookId));
        if (alreadyReserved) {
            throw new ConflictException("That member has already reserved this book");
        }

        Reservation saved = reservations.save(new Reservation(bookId, memberId, clock.instant()));
        return toView(saved);
    }

    @Transactional
    public ReservationView cancel(long reservationId) {
        Reservation reservation = reservations
                .findById(reservationId)
                .orElseThrow(() -> new NotFoundException("No reservation with id " + reservationId));
        if (!reservation.isActive()) {
            throw new ConflictException("That reservation was already cancelled");
        }
        reservation.cancelOn(clock.instant());
        return toView(reservations.save(reservation));
    }

    private ReservationView toView(Reservation reservation) {
        BookView book = books.get(reservation.getBookId());
        MemberView member = members.get(reservation.getMemberId());
        boolean active = reservation.isActive();
        Integer queuePosition = active ? queuePositionOf(reservation) : null;
        return new ReservationView(
                reservation.getId(),
                reservation.getBookId(),
                book.title(),
                reservation.getMemberId(),
                member.name(),
                reservation.getReservedAt(),
                active ? "ACTIVE" : "CANCELLED",
                queuePosition);
    }

    private Integer queuePositionOf(Reservation reservation) {
        List<Reservation> active =
                reservations.findByBookIdAndCancelledAtIsNullOrderByReservedAtAscIdAsc(reservation.getBookId());
        for (int i = 0; i < active.size(); i++) {
            if (active.get(i).getId().equals(reservation.getId())) {
                return i + 1;
            }
        }
        return null;
    }
}
