package com.konrad.library.lending;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.konrad.library.common.ConflictException;
import com.konrad.library.lending.service.LoanService;
import com.konrad.library.lending.service.LoanView;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class LendingFlowTest {

    @Autowired
    private LoanService loans;

    @Test
    void borrowingRecordsTheBookTheMemberAndADueDate() {
        LoanView loan = loans.borrow(1L, 1L);

        assertThat(loan.bookTitle()).isEqualTo("Clean Code");
        assertThat(loan.memberName()).isEqualTo("Priya Raman");
        assertThat(loan.returnedAt()).isNull();
        assertThat(loan.dueAt()).isAfter(loan.borrowedAt());
    }

    @Test
    void theLastCopyCannotBeBorrowedTwice() {
        // Book 4 has one copy and it is already out in the seed data.
        assertThatThrownBy(() -> loans.borrow(4L, 2L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("on the shelf");
    }

    @Test
    void aMemberCannotHoldTwoCopiesOfTheSameTitle() {
        loans.borrow(1L, 1L);

        assertThatThrownBy(() -> loans.borrow(1L, 1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already has this book");
    }

    @Test
    void returningFreesTheCopyAndStampsTheReturnDate() {
        LoanView borrowed = loans.borrow(2L, 3L);

        LoanView returned = loans.returnLoan(borrowed.id());

        assertThat(returned.returnedAt()).isNotNull();
        assertThatThrownBy(() -> loans.returnLoan(borrowed.id()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already returned");
    }
}
