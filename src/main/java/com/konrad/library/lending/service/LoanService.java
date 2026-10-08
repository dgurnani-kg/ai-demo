package com.konrad.library.lending.service;

import com.konrad.library.catalog.service.BookService;
import com.konrad.library.catalog.service.BookView;
import com.konrad.library.common.ConflictException;
import com.konrad.library.common.NotFoundException;
import com.konrad.library.lending.domain.Loan;
import com.konrad.library.lending.domain.LoanRepository;
import com.konrad.library.membership.service.MemberService;
import com.konrad.library.membership.service.MemberView;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class LoanService {

    private final LoanRepository loans;
    private final BookService books;
    private final MemberService members;
    private final Clock clock;

    public LoanService(LoanRepository loans, BookService books, MemberService members, Clock clock) {
        this.loans = loans;
        this.books = books;
        this.members = members;
        this.clock = clock;
    }

    public List<LoanView> list(Long memberId) {
        List<Loan> found = memberId == null
                ? loans.findAllByOrderByBorrowedAtDesc()
                : loans.findByMemberIdOrderByBorrowedAtDesc(memberId);
        return found.stream().map(this::toView).toList();
    }

    @Transactional
    public LoanView borrow(long bookId, long memberId) {
        BookView book = books.get(bookId);
        members.get(memberId);

        if (book.availableCopies() <= 0) {
            throw new ConflictException("No copies of \"" + book.title() + "\" are on the shelf");
        }
        boolean alreadyHasIt = loans.findByMemberIdOrderByBorrowedAtDesc(memberId).stream()
                .anyMatch(loan -> loan.isOpen() && loan.getBookId().equals(bookId));
        if (alreadyHasIt) {
            throw new ConflictException("That member already has this book on loan");
        }

        Loan saved = loans.save(new Loan(bookId, memberId, clock.instant()));
        return toView(saved);
    }

    @Transactional
    public LoanView returnLoan(long loanId) {
        Loan loan = loans.findById(loanId).orElseThrow(() -> new NotFoundException("No loan with id " + loanId));
        if (!loan.isOpen()) {
            throw new ConflictException("That loan was already returned");
        }
        loan.returnOn(clock.instant());
        return toView(loans.save(loan));
    }

    private LoanView toView(Loan loan) {
        BookView book = books.get(loan.getBookId());
        MemberView member = members.get(loan.getMemberId());
        return new LoanView(
                loan.getId(),
                loan.getBookId(),
                book.title(),
                loan.getMemberId(),
                member.name(),
                loan.getBorrowedAt(),
                loan.getDueAt(),
                loan.getReturnedAt());
    }
}
