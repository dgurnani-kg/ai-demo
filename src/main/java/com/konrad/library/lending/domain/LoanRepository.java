package com.konrad.library.lending.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    long countByBookIdAndReturnedAtIsNull(Long bookId);

    List<Loan> findByMemberIdOrderByBorrowedAtDesc(Long memberId);

    List<Loan> findAllByOrderByBorrowedAtDesc();
}
