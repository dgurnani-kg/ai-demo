package com.konrad.library.catalog;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class BookEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listsTheWholeCatalogueAlphabetically() throws Exception {
        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(5)))
                .andExpect(jsonPath("$[0].title").value("Building Microservices"));
    }

    @Test
    void reportsAvailabilityNetOfOpenLoans() throws Exception {
        // Seed data has one of the single copy of Building Microservices already out.
        mockMvc.perform(get("/api/books/{bookId}", 4))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCopies").value(1))
                .andExpect(jsonPath("$.availableCopies").value(0));
    }

    @Test
    void availabilityFilterHidesBooksWithNoCopiesOnTheShelf() throws Exception {
        mockMvc.perform(get("/api/books").param("available", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.title=='Building Microservices')]").isEmpty());
    }

    @Test
    void unknownBookIsANotFound() throws Exception {
        mockMvc.perform(get("/api/books/{bookId}", 9999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
