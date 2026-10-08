package com.konrad.library.catalog.service;

import com.konrad.library.catalog.domain.Book;
import com.konrad.library.catalog.domain.BookRepository;
import com.konrad.library.common.NotFoundException;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BookService {

    private final BookRepository books;
    private final CopiesOnLoan copiesOnLoan;

    public BookService(BookRepository books, CopiesOnLoan copiesOnLoan) {
        this.books = books;
        this.copiesOnLoan = copiesOnLoan;
    }

    public List<BookView> list(boolean onlyAvailable) {
        return books.findAll().stream()
                .map(this::toView)
                .filter(view -> !onlyAvailable || view.availableCopies() > 0)
                .sorted(Comparator.comparing(BookView::title))
                .toList();
    }

    public BookView get(long bookId) {
        return books.findById(bookId)
                .map(this::toView)
                .orElseThrow(() -> new NotFoundException("No book with id " + bookId));
    }

    private BookView toView(Book book) {
        int onLoan = copiesOnLoan.countOnLoan(book.getId());
        return new BookView(
                book.getId(),
                book.getIsbn(),
                book.getTitle(),
                book.getAuthor(),
                book.getTotalCopies(),
                book.getTotalCopies() - onLoan);
    }
}
