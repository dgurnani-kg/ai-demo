package com.konrad.library.contract;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

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
    void placeReservationMatchesSpec() throws Exception {
        long reservationId = joinTheQueueForBookFour();

        cancel(reservationId);
    }

    @Test
    void reservingAShelvedTitleErrorMatchesSpec() throws Exception {
        // Effective Java has four copies and none of them are out, so there is nothing to wait for.
        mockMvc.perform(post("/api/books/{bookId}/reservations", 3)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\":1}"))
                .andExpect(status().isConflict())
                .andExpect(OpenApiValidationMatchers.openApi().isValid(SPEC));
    }

    @Test
    void reservingAnUnknownBookErrorMatchesSpec() throws Exception {
        mockMvc.perform(post("/api/books/{bookId}/reservations", 9999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\":1}"))
                .andExpect(status().isNotFound())
                .andExpect(OpenApiValidationMatchers.openApi().isValid(SPEC));
    }

    @Test
    void listReservationsMatchesSpec() throws Exception {
        mockMvc.perform(get("/api/reservations"))
                .andExpect(status().isOk())
                .andExpect(OpenApiValidationMatchers.openApi().isValid(SPEC));
    }

    @Test
    void listReservationsForOneMemberMatchesSpec() throws Exception {
        mockMvc.perform(get("/api/reservations").param("memberId", "2"))
                .andExpect(status().isOk())
                .andExpect(OpenApiValidationMatchers.openApi().isValid(SPEC));
    }

    @Test
    void listReservationsByStatusMatchesSpec() throws Exception {
        mockMvc.perform(get("/api/reservations").param("status", "WAITING"))
                .andExpect(status().isOk())
                .andExpect(OpenApiValidationMatchers.openApi().isValid(SPEC));
    }

    @Test
    void cancelReservationMatchesSpec() throws Exception {
        long reservationId = joinTheQueueForBookFour();

        cancel(reservationId)
                .andExpect(status().isOk())
                .andExpect(OpenApiValidationMatchers.openApi().isValid(SPEC));
    }

    @Test
    void cancellingAReservationTwiceErrorMatchesSpec() throws Exception {
        long reservationId = joinTheQueueForBookFour();
        cancel(reservationId);

        cancel(reservationId)
                .andExpect(status().isConflict())
                .andExpect(OpenApiValidationMatchers.openApi().isValid(SPEC));
    }

    @Test
    void cancellingAnUnknownReservationErrorMatchesSpec() throws Exception {
        mockMvc.perform(post("/api/reservations/{reservationId}/cancel", 999999))
                .andExpect(status().isNotFound())
                .andExpect(OpenApiValidationMatchers.openApi().isValid(SPEC));
    }

    /**
     * Aisha joins the queue for Building Microservices, whose only copy is out in the seed data.
     * This class is not transactional, so every caller cancels the place it took again: a
     * CANCELLED reservation is not an active one, which leaves the queue as it was found no
     * matter what order the methods run in.
     */
    private long joinTheQueueForBookFour() throws Exception {
        String body = mockMvc.perform(post("/api/books/{bookId}/reservations", 4)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\":3}"))
                .andExpect(status().isCreated())
                .andExpect(OpenApiValidationMatchers.openApi().isValid(SPEC))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private ResultActions cancel(long reservationId) throws Exception {
        return mockMvc.perform(post("/api/reservations/{reservationId}/cancel", reservationId));
    }
}
