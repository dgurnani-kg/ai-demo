package com.konrad.library.lending;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.konrad.library.common.ConflictException;
import com.konrad.library.common.NotFoundException;
import com.konrad.library.lending.service.LoanService;
import com.konrad.library.lending.service.LoanView;
import com.konrad.library.lending.service.ReservationService;
import com.konrad.library.lending.service.ReservationView;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * The waiting queue, told the way a librarian would tell it.
 *
 * <p>Everything hangs off the seed data: Building Microservices has a single copy, that copy is
 * out on loan 1 to Priya, and Tomas is already waiting in the queue for it (reservation 1). So it
 * is the one title that can be reserved without arranging anything first.
 */
@SpringBootTest
@Transactional
class ReservationFlowTest {

    /** Building Microservices: one copy, and it is out in the seed data. */
    private static final long FULLY_OUT_BOOK = 4L;

    /** Effective Java: four copies, none of them out. */
    private static final long SHELVED_BOOK = 3L;

    private static final long PRIYA = 1L;
    private static final long TOMAS = 2L;
    private static final long AISHA = 3L;

    /** Priya's open loan of the only copy of Building Microservices. */
    private static final long OPEN_LOAN = 1L;

    /** Tomas's seeded place in the queue for Building Microservices. */
    private static final long TOMAS_QUEUE_PLACE = 1L;

    @Autowired
    private ReservationService reservations;

    @Autowired
    private LoanService loans;

    @Test
    void reservingATitleThatIsFullyOutJoinsTheQueue() {
        ReservationView placed = reservations.reserve(FULLY_OUT_BOOK, AISHA);

        assertThat(placed.bookTitle()).isEqualTo("Building Microservices");
        assertThat(placed.memberName()).isEqualTo("Aisha Okonkwo");
        assertThat(placed.status()).isEqualTo("WAITING");
        assertThat(placed.readyAt()).isNull();
    }

    @Test
    void aTitleWithCopiesOnTheShelfCannotBeReserved() {
        assertThatThrownBy(() -> reservations.reserve(SHELVED_BOOK, PRIYA))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("on the shelf");
    }

    @Test
    void aMemberCannotJoinTheSameQueueTwice() {
        reservations.reserve(FULLY_OUT_BOOK, AISHA);

        assertThatThrownBy(() -> reservations.reserve(FULLY_OUT_BOOK, AISHA))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already in the queue");
    }

    @Test
    void aMemberWhoAlreadyHasTheBookOnLoanCannotReserveIt() {
        // Priya is holding the only copy, so there is nothing for her to wait for.
        assertThatThrownBy(() -> reservations.reserve(FULLY_OUT_BOOK, PRIYA))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already has this book on loan");
    }

    @Test
    void reservingABookThatDoesNotExistIsNotFound() {
        assertThatThrownBy(() -> reservations.reserve(9999L, AISHA)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void reservingForAMemberWhoDoesNotExistIsNotFound() {
        assertThatThrownBy(() -> reservations.reserve(FULLY_OUT_BOOK, 9999L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void returningTheCopyHoldsItForTheMemberWhoWaitedLongest() {
        loans.returnLoan(OPEN_LOAN);

        ReservationView held = reload(TOMAS_QUEUE_PLACE);
        assertThat(held.status()).isEqualTo("READY");
        assertThat(held.readyAt()).isNotNull();
    }

    @Test
    void returningOneCopyPromotesOnlyOneMemberInTheQueue() {
        ReservationView behindTomas = reservations.reserve(FULLY_OUT_BOOK, AISHA);

        loans.returnLoan(OPEN_LOAN);

        assertThat(reload(behindTomas.id()).status()).isEqualTo("WAITING");
    }

    @Test
    void aMemberWhoIsNotHoldingTheCopyCannotBorrowIt() {
        loans.returnLoan(OPEN_LOAN);

        assertThatThrownBy(() -> loans.borrow(FULLY_OUT_BOOK, AISHA))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("being held for a member who reserved it");
    }

    @Test
    void theMemberTheCopyIsHeldForCanBorrowIt() {
        loans.returnLoan(OPEN_LOAN);

        LoanView loan = loans.borrow(FULLY_OUT_BOOK, TOMAS);

        assertThat(loan.memberName()).isEqualTo("Tomas Lindgren");
        assertThat(loan.bookTitle()).isEqualTo("Building Microservices");
    }

    @Test
    void borrowingTheHeldCopyClosesTheReservationAsCollected() {
        loans.returnLoan(OPEN_LOAN);

        loans.borrow(FULLY_OUT_BOOK, TOMAS);

        assertThat(reload(TOMAS_QUEUE_PLACE).status()).isEqualTo("COLLECTED");
    }

    @Test
    void cancellingAReservationGivesUpThePlaceInTheQueue() {
        ReservationView cancelled = reservations.cancel(TOMAS_QUEUE_PLACE);

        assertThat(cancelled.status()).isEqualTo("CANCELLED");
    }

    @Test
    void aReservationCannotBeCancelledTwice() {
        reservations.cancel(TOMAS_QUEUE_PLACE);

        assertThatThrownBy(() -> reservations.cancel(TOMAS_QUEUE_PLACE))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("no longer active");
    }

    @Test
    void cancellingAHeldCopyPassesItToTheNextMemberInTheQueue() {
        ReservationView behindTomas = reservations.reserve(FULLY_OUT_BOOK, AISHA);
        loans.returnLoan(OPEN_LOAN);

        reservations.cancel(TOMAS_QUEUE_PLACE);

        assertThat(reload(behindTomas.id()).status()).isEqualTo("READY");
    }

    @Test
    void aMemberWhoGaveUpTheirPlaceCanJoinTheQueueAgain() {
        ReservationView first = reservations.reserve(FULLY_OUT_BOOK, AISHA);
        reservations.cancel(first.id());

        ReservationView second = reservations.reserve(FULLY_OUT_BOOK, AISHA);

        assertThat(second.status()).isEqualTo("WAITING");
    }

    @Test
    void cancellingAReservationThatDoesNotExistIsNotFound() {
        assertThatThrownBy(() -> reservations.cancel(9999L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void theListCanBeNarrowedToOneMember() {
        List<ReservationView> tomas = reservations.list(TOMAS, null);

        assertThat(tomas).isNotEmpty().allSatisfy(view -> assertThat(view.memberId())
                .isEqualTo(TOMAS));
    }

    @Test
    void theStatusFilterIgnoresCase() {
        List<ReservationView> lowerCase = reservations.list(null, "waiting");

        assertThat(lowerCase).isNotEmpty().isEqualTo(reservations.list(null, "WAITING"));
    }

    @Test
    void anUnrecognisedStatusMatchesNothingRatherThanFailing() {
        assertThat(reservations.list(null, "ON_A_SHELF_SOMEWHERE")).isEmpty();
    }

    /** Reads a reservation back through the public API, so the test never touches a repository. */
    private ReservationView reload(long reservationId) {
        return reservations.list(null, null).stream()
                .filter(view -> view.id() == reservationId)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No reservation with id " + reservationId));
    }
}
