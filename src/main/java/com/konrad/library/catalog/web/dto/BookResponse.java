package com.konrad.library.catalog.web.dto;

import com.konrad.library.catalog.service.BookView;

/** Wire shape for Book. Must stay identical to the Book schema in api/openapi.yaml. */
public record BookResponse(Long id, String isbn, String title, String author, int totalCopies, int availableCopies) {

    public static BookResponse from(BookView view) {
        return new BookResponse(
                view.id(), view.isbn(), view.title(), view.author(), view.totalCopies(), view.availableCopies());
    }
}
