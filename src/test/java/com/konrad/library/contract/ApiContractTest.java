package com.konrad.library.contract;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

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
}
