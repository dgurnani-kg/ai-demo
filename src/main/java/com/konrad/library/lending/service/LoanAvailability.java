package com.konrad.library.lending.service;

import com.konrad.library.catalog.service.CopiesOnLoan;
import com.konrad.library.lending.domain.LoanRepository;
import org.springframework.stereotype.Service;

/** Lending's answer to the catalogue's question. The only direction this dependency runs. */
@Service
public class LoanAvailability implements CopiesOnLoan {

    private final LoanRepository loans;

    public LoanAvailability(LoanRepository loans) {
        this.loans = loans;
    }

    @Override
    public int countOnLoan(long bookId) {
        return (int) loans.countByBookIdAndReturnedAtIsNull(bookId);
    }
}
