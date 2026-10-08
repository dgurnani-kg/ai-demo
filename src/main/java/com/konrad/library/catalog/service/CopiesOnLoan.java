package com.konrad.library.catalog.service;

/**
 * How many copies of a book are currently out.
 *
 * <p>Declared here and implemented in the lending feature so that the dependency runs
 * lending -> catalog only. Flip it and ArchitectureTest.noCyclesBetweenFeatures fails.
 */
public interface CopiesOnLoan {

    int countOnLoan(long bookId);
}
