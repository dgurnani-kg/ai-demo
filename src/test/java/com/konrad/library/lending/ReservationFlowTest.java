package com.konrad.library.lending;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.konrad.library.common.ConflictException;
import com.konrad.library.common.NotFoundException;
import com.konrad.library.lending.service.ReservationService;
import com.konrad.library.lending.service.ReservationView;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ReservationFlowTest {

    @Autowired
    private ReservationService reservations;

    @Test
    void reservingABookWithNoCopiesOnTheShelfRecordsTheBookTheMemberAndFirstPlaceInQueue() {
        // Book 4 has one copy and it is already out in the seed data.
        ReservationView reservation = reservations.reserve(4L, 2L);

        assertThat(reservation.bookTitle()).isEqualTo("Building Microservices");
        assertThat(reservation.memberName()).isEqualTo("Tomas Lindgren");
        assertThat(reservation.status()).isEqualTo("ACTIVE");
        assertThat(reservation.queuePosition()).isEqualTo(1);
    }

    @Test
    void theQueueIsOrderedByWhenTheBookWasReserved() {
        reservations.reserve(4L, 2L);
        ReservationView second = reservations.reserve(4L, 3L);

        assertThat(second.queuePosition()).isEqualTo(2);
    }

    @Test
    void aBookWithCopiesOnTheShelfCannotBeReserved() {
        // Book 1 (Clean Code) has copies available - borrow it instead.
        assertThatThrownBy(() -> reservations.reserve(1L, 2L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("on the shelf");
    }

    @Test
    void aMemberWhoAlreadyHasTheBookOnLoanCannotReserveIt() {
        // Member 1 holds the only copy of book 4 in the seed data.
        assertThatThrownBy(() -> reservations.reserve(4L, 1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already has this book on loan");
    }

    @Test
    void theSameMemberCannotReserveTheSameBookTwice() {
        reservations.reserve(4L, 2L);

        assertThatThrownBy(() -> reservations.reserve(4L, 2L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already reserved");
    }

    @Test
    void reservingAnUnknownBookIsNotFound() {
        assertThatThrownBy(() -> reservations.reserve(9999L, 2L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void reservingForAnUnknownMemberIsNotFound() {
        assertThatThrownBy(() -> reservations.reserve(4L, 9999L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void cancellingFlipsTheStatusAndClearsTheQueuePosition() {
        ReservationView reservation = reservations.reserve(4L, 2L);

        ReservationView cancelled = reservations.cancel(reservation.id());

        assertThat(cancelled.status()).isEqualTo("CANCELLED");
        assertThat(cancelled.queuePosition()).isNull();
    }

    @Test
    void cancellingTheSameReservationTwiceIsAConflict() {
        ReservationView reservation = reservations.reserve(4L, 2L);
        reservations.cancel(reservation.id());

        assertThatThrownBy(() -> reservations.cancel(reservation.id()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already cancelled");
    }

    @Test
    void cancellingAnUnknownReservationIsNotFound() {
        assertThatThrownBy(() -> reservations.cancel(9999L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void cancellingTheFirstInQueueMovesTheSecondUpToFirst() {
        ReservationView first = reservations.reserve(4L, 2L);
        ReservationView second = reservations.reserve(4L, 3L);

        reservations.cancel(first.id());

        // Re-read via list(...) rather than reusing the stale "second" view. Find our own
        // reservation by id instead of asserting on the full list's size: ApiContractTest is
        // not @Transactional, so when the whole suite runs in one JVM it can leave its own
        // (cancelled) book-4 reservations sitting in the shared H2 instance alongside these.
        ReservationView updatedSecond = reservations.list(3L).stream()
                .filter(reservation -> reservation.id().equals(second.id()))
                .findFirst()
                .orElseThrow();
        assertThat(updatedSecond.queuePosition()).isEqualTo(1);
    }

    @Test
    void listingByMemberReturnsOnlyThatMembersReservations() {
        // Book 4 is the only seeded book with nothing on the shelf, so both reservations
        // target it; member identity is what list(memberId) filters on.
        ReservationView mine = reservations.reserve(4L, 2L);
        reservations.reserve(4L, 3L);

        List<ReservationView> forMemberTwo = reservations.list(2L);

        // Every entry belongs to member 2 (member 3's reservation is excluded), and our own
        // reservation is among them. Not asserting hasSize(1): ApiContractTest's reservation
        // rows for book 4 persist in the shared H2 instance for the rest of the suite run.
        assertThat(forMemberTwo).extracting(ReservationView::memberId).containsOnly(2L);
        assertThat(forMemberTwo).extracting(ReservationView::id).contains(mine.id());
    }

    @Test
    void listingWithNoMemberReturnsEveryReservation() {
        ReservationView first = reservations.reserve(4L, 2L);
        ReservationView second = reservations.reserve(4L, 3L);

        List<ReservationView> all = reservations.list(null);

        // list(null) is not filtered by member - both of ours show up regardless of whatever
        // else is in the shared H2 instance for this suite run.
        assertThat(all).extracting(ReservationView::id).contains(first.id(), second.id());
    }
}
