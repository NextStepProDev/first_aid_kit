package com.drugs.controller.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ValidationErrorWebIntegrationTest {

    @Autowired
    @SuppressWarnings("unused")
    private MockMvc mockMvc;

    @Test
    @DisplayName("Should return 400 and validation error for invalid drug form")
    void shouldReturnValidationErrorForInvalidDrugForm() throws Exception {
        int currentYear = OffsetDateTime.now().getYear();

        String requestJson = """
                {
                    "name": "Ibuprofen",
                    "form": "FORM_INVALID",
                    "description": "Painkiller",
                    "expirationYear": %d,
                    "expirationMonth": 12
                }
                """.formatted(currentYear);

        mockMvc.perform(post("/api/drugs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.errors[0].field").value("form"))
                .andExpect(jsonPath("$.errors[0].rejectedValue").value("FORM_INVALID"))
                .andExpect(jsonPath("$.errors[0].message").exists());
    }

    @Test
    @DisplayName("Should return 400 and validation error for blank description")
    void shouldReturnValidationErrorForBlankDescription() throws Exception {
        int currentYear = OffsetDateTime.now().getYear();

        String requestJson = """
                {
                    "name": "Ibuprofen",
                    "form": "GEL",
                    "description": "",
                    "expirationYear": %d,
                    "expirationMonth": 12
                }
                """.formatted(currentYear);

        String requestJson2 = """
                {
                    "name": "Ibuprofen",
                    "form": "GEL",
                    "description": null,
                    "expirationYear": %d,
                    "expirationMonth": 12
                }
                """.formatted(currentYear);

        mockMvc.perform(post("/api/drugs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.errors[0].field").value("description"))
                .andExpect(jsonPath("$.errors[0].rejectedValue").value(""))
                .andExpect(jsonPath("$.errors[0].message").exists());

        mockMvc.perform(post("/api/drugs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson2))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.errors[0].field").value("description"))
                .andExpect(jsonPath("$.errors[0].rejectedValue").doesNotExist())
                .andExpect(jsonPath("$.errors[0].message").exists());
    }

    @Test
    @DisplayName("Should return 400 and validation error for wrong month")
    void shouldReturnValidationErrorForWrongMonth() throws Exception {
        int currentYear = OffsetDateTime.now().getYear();

        String requestJson = """
                {
                    "name": "Ibuprofen",
                    "form": "GEL",
                    "description": "Painkiller",
                    "expirationYear": %d,
                    "expirationMonth": 13
                }
                """.formatted(currentYear);

        String requestJson2 = """
                {
                    "name": "Ibuprofen",
                    "form": "GEL",
                    "description": "Painkiller",
                    "expirationYear": %d,
                    "expirationMonth": 0
                }
                """.formatted(currentYear);

        String requestJson3 = """
                {
                    "name": "Ibuprofen",
                    "form": "GEL",
                    "description": "Painkiller",
                    "expirationYear": %d,
                    "expirationMonth": -1
                }
                """.formatted(currentYear);

        mockMvc.perform(post("/api/drugs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.errors[0].field").value("expirationMonth"))
                .andExpect(jsonPath("$.errors[0].rejectedValue").value(13))
                .andExpect(jsonPath("$.errors[0].message").exists());

        mockMvc.perform(post("/api/drugs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson2))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.errors[0].field").value("expirationMonth"))
                .andExpect(jsonPath("$.errors[0].rejectedValue").value(0))
                .andExpect(jsonPath("$.errors[0].message").exists());

        mockMvc.perform(post("/api/drugs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson3))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.errors[0].field").value("expirationMonth"))
                .andExpect(jsonPath("$.errors[0].rejectedValue").value(-1))
                .andExpect(jsonPath("$.errors[0].message").exists());
    }
    @Test
    @DisplayName("Should return 400 and validation error for blank description")
    void shouldReturnValidationErrorForWrongYear() throws Exception {
        String requestJson = """
                {
                    "name": "Ibuprofen",
                    "form": "GEL",
                    "description": "Painkiller",
                    "expirationYear": 2020,
                    "expirationMonth": 12
                }
                """;

        String requestJson2 = """
                {
                    "name": "Ibuprofen",
                    "form": "GEL",
                    "description": "Painkiller",
                    "expirationYear": 1023,
                    "expirationMonth": 12
                }
                """;

        mockMvc.perform(post("/api/drugs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.errors[0].field").value("expirationYear"))
                .andExpect(jsonPath("$.errors[0].rejectedValue").value(2020))
                .andExpect(jsonPath("$.errors[0].message").exists());

        mockMvc.perform(post("/api/drugs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson2))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.errors[0].field").value("expirationYear"))
                .andExpect(jsonPath("$.errors[0].rejectedValue").value(1023))
                .andExpect(jsonPath("$.errors[0].message").exists());
    }
}