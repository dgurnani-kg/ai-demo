package com.konrad.library.catalog.web;

import com.konrad.library.catalog.service.BookService;
import com.konrad.library.catalog.web.dto.BookResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/books")
public class BookController {

    private final BookService books;

    public BookController(BookService books) {
        this.books = books;
    }

    @GetMapping
    public List<BookResponse> listBooks(
            @RequestParam(name = "available", required = false, defaultValue = "false") boolean available) {
        return books.list(available).stream().map(BookResponse::from).toList();
    }

    @GetMapping("/{bookId}")
    public BookResponse getBook(@PathVariable long bookId) {
        return BookResponse.from(books.get(bookId));
    }
}
