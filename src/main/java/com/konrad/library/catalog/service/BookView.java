package com.konrad.library.catalog.service;

/**
 * What the catalogue knows about a book once availability has been worked out.
 * Shaped to match the Book schema in api/openapi.yaml.
 */
public record BookView(Long id, String isbn, String title, String author, int totalCopies, int availableCopies) {}
