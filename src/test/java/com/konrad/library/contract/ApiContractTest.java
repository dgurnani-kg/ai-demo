package com.konrad.library.contract;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Every response is checked against api/openapi.yaml.
 *
 * <p>The schemas in that file set {@code additionalProperties: false}, so a field the backend
 * invented and the spec never promised fails here — which is the point. The contract is the
 * boundary between agents working in parallel; this test is what makes the boundary real.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApiContractTest {

    private static final String SPEC = "api/openapi.yaml";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void specItselfIsValid() {
        OpenApiInteractionValidator.createFor(SPEC).build();
    }

    @Test
    void listBooksMatchesSpec() throws Exception {
        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(OpenApiValidationMatchers.openApi().isValid(SPEC));
    }

    @Test
    void listBooksFilteredByAvailabilityMatchesSpec() throws Exception {
        mockMvc.perform(get("/api/books").param("available", "true"))
                .andExpect(status().isOk())
                .andExpect(OpenApiValidationMatchers.openApi().isValid(SPEC));
    }

    @Test
    void getBookMatchesSpec() throws Exception {
        mockMvc.perform(get("/api/books/{bookId}", 1))
                .andExpect(status().isOk())
                .andExpect(OpenApiValidationMatchers.openApi().isValid(SPEC));
    }

    @Test
    void missingBookErrorMatchesSpec() throws Exception {
        mockMvc.perform(get("/api/books/{bookId}", 9999))
                .andExpect(status().isNotFound())
                .andExpect(OpenApiValidationMatchers.openApi().isValid(SPEC));
    }

    @Test
    void listMembersMatchesSpec() throws Exception {
        mockMvc.perform(get("/api/members"))
                .andExpect(status().isOk())
                .andExpect(OpenApiValidationMatchers.openApi().isValid(SPEC));
    }

    @Test
    void listLoansMatchesSpec() throws Exception {
        mockMvc.perform(get("/api/loans"))
                .andExpect(status().isOk())
                .andExpect(OpenApiValidationMatchers.openApi().isValid(SPEC));
    }

    @Test
    void borrowMatchesSpec() throws Exception {
        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\":1,\"memberId\":2}"))
                .andExpect(status().isCreated())
                .andExpect(OpenApiValidationMatchers.openApi().isValid(SPEC));
    }

    @Test
    void reserveMatchesSpec() throws Exception {
        // Book 4 has no copies on the shelf in the seed data, so it can be reserved.
        MvcResult created = mockMvc.perform(post("/api/books/{bookId}/reservations", 4)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\":2}"))
                .andExpect(status().isCreated())
                .andExpect(OpenApiValidationMatchers.openApi().isValid(SPEC))
                .andReturn();

        // This class is not @Transactional, so the reservation above is committed to the shared
        // H2 instance for the rest of the test run. Cancel it so member 2 does not permanently
        // hold book 4, which would otherwise collide with ReservationFlowTest's own use of
        // book 4 / member 2 whenever both test classes run in the same JVM.
        long reservationId = objectMapper
                .readTree(created.getResponse().getContentAsString())
                .get("id")
                .asLong();
        mockMvc.perform(post("/api/reservations/{reservationId}/cancel", reservationId))
                .andExpect(status().isOk());
    }

    @Test
    void listReservationsMatchesSpec() throws Exception {
        mockMvc.perform(get("/api/reservations"))
                .andExpect(status().isOk())
                .andExpect(OpenApiValidationMatchers.openApi().isValid(SPEC));
    }

    @Test
    void cancelReservationMatchesSpec() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/books/{bookId}/reservations", 4)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\":3}"))
                .andExpect(status().isCreated())
                .andReturn();

        long reservationId = objectMapper
                .readTree(created.getResponse().getContentAsString())
                .get("id")
                .asLong();

        mockMvc.perform(post("/api/reservations/{reservationId}/cancel", reservationId))
                .andExpect(status().isOk())
                .andExpect(OpenApiValidationMatchers.openApi().isValid(SPEC));
    }

    @Test
    void reservingAnAvailableBookErrorMatchesSpec() throws Exception {
        // Book 1 (Clean Code) has copies available, so it cannot be reserved.
        mockMvc.perform(post("/api/books/{bookId}/reservations", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\":1}"))
                .andExpect(status().isConflict())
                .andExpect(OpenApiValidationMatchers.openApi().isValid(SPEC));
    }
}
