package com.konrad.library.lending.web;

import com.konrad.library.lending.service.LoanService;
import com.konrad.library.lending.web.dto.BorrowRequest;
import com.konrad.library.lending.web.dto.LoanResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/loans")
public class LoanController {

    private final LoanService loans;

    public LoanController(LoanService loans) {
        this.loans = loans;
    }

    @GetMapping
    public List<LoanResponse> listLoans(@RequestParam(name = "memberId", required = false) Long memberId) {
        return loans.list(memberId).stream().map(LoanResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LoanResponse borrowBook(@Valid @RequestBody BorrowRequest request) {
        return LoanResponse.from(loans.borrow(request.bookId(), request.memberId()));
    }

    @PostMapping("/{loanId}/return")
    public LoanResponse returnLoan(@PathVariable long loanId) {
        return LoanResponse.from(loans.returnLoan(loanId));
    }
}
