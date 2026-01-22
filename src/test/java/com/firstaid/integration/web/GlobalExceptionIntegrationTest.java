package com.firstaid.integration.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.firstaid.config.NoSecurityConfig;
import com.firstaid.config.TestCacheConfig;
import com.firstaid.config.TestSecurityConfig;
import com.firstaid.controller.drug.DrugController;
import com.firstaid.controller.alert.AlertController;
import com.firstaid.controller.handler.GlobalExceptionHandler;
import com.firstaid.domain.exception.DrugNotFoundException;
import com.firstaid.domain.exception.EmailSendingException;
import com.firstaid.domain.exception.InvalidSortFieldException;
import com.firstaid.infrastructure.cache.UserAwareCacheKeyGenerator;
import com.firstaid.infrastructure.pdf.PdfExportService;
import com.firstaid.infrastructure.security.JwtAuthenticationFilter;
import com.firstaid.infrastructure.security.JwtTokenProvider;
import com.firstaid.service.DrugService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Objects;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {DrugController.class, AlertController.class})
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, NoSecurityConfig.class, TestCacheConfig.class, TestSecurityConfig.class})
public class GlobalExceptionIntegrationTest {

    static int CURRENT_YEAR = OffsetDateTime.now().getYear();
    static int NEXT_MONTH = OffsetDateTime.now().getMonthValue() + 1;

    @Autowired
    @SuppressWarnings("unused")
    private MockMvc mockMvc;

    @Autowired
    @SuppressWarnings("unused")
    private ObjectMapper objectMapper;

    @MockitoBean
    @SuppressWarnings("unused")
    private DrugService drugService;

    @MockitoBean
    @SuppressWarnings("unused")
    private PdfExportService pdfExportService;

    @MockitoBean
    @SuppressWarnings("unused")
    private UserAwareCacheKeyGenerator userAwareCacheKeyGenerator;

    @MockitoBean
    @SuppressWarnings("unused")
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    @SuppressWarnings("unused")
    private JwtTokenProvider jwtTokenProvider;

    @Nested
    @DisplayName("Validation errors (@Valid DTO)")
    class ValidationErrorsTest {

        @Test
        void shouldReturn400WhenExpirationYearIsInThePast() throws Exception {
            Map<String, Object> invalidDto = Map.of(
                    "name", "Ibuprofen",
                    "form", "PILLS",
                    "description", "Painkiller",
                    "expirationYear", 1000,  // za mały
                    "expirationMonth", NEXT_MONTH
            );

            mockMvc.perform(post("/api/drugs")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[*].field").value(org.hamcrest.Matchers.hasItem("expirationYear")));
        }

        @Test
        void shouldReturn400WhenDtoValidationFails() throws Exception {
            Map<String, Object> invalidDto = Map.of(
                    "name", "",
                    "form", "INVALID_FORM",
                    "expirationYear", CURRENT_YEAR,
                    "expirationMonth", NEXT_MONTH
            );

            mockMvc.perform(post("/api/drugs")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.errors").isNotEmpty())
                    .andExpect(jsonPath("$.message").value("Validation failed"))
                    .andExpect(jsonPath("$.errors").isArray())
                    .andExpect(jsonPath("$.errors").isNotEmpty());
        }

        @Test
        void shouldReturn400WhenDescriptionIsBlank() throws Exception {
            Map<String, Object> invalidDto = Map.of(
                    "name", "Ibuprofen",
                    "form", "PILLS",
                    "description", "",  // <---
                    "expirationYear", CURRENT_YEAR,
                    "expirationMonth", NEXT_MONTH
            );

            mockMvc.perform(post("/api/drugs")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Validation failed"))
                    .andExpect(jsonPath("$.errors").isArray())
                    .andExpect(jsonPath("$.errors[*].field").value(org.hamcrest.Matchers.hasItem("description")));
        }

        @Test
        void shouldReturn400WhenExpirationMonthIsOutOfRange() throws Exception {
            Map<String, Object> invalidDto = Map.of(
                    "name", "Ibuprofen",
                    "form", "PILLS",
                    "description", "Painkiller",
                    "expirationYear", CURRENT_YEAR,
                    "expirationMonth", 15
            );

            mockMvc.perform(post("/api/drugs")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[*].field").value(org.hamcrest.Matchers.hasItem("expirationMonth")));
        }
    }

    @Nested
    @DisplayName("Malformed JSON")
    class MalformedJsonTest {

        @Test
        void shouldReturn400ForUnreadableJson() throws Exception {
            // given
            String malformedJson = """
                    { 
                        "name": "Paracetamol", 
                        "form": "PILLS"
                    """; // Missing closing brace

            // when & then
            mockMvc.perform(post("/api/drugs")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(malformedJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("Request body is invalid or unreadable"));
        }
    }

    @Nested
    @DisplayName("Invalid Request Parameters")
    class ConstraintViolationTest {

        @Test
        @DisplayName("shouldReturn400ForTypeMismatchInPathVariable (id not a number)")
        void shouldReturn400ForTypeMismatchInPathVariable() throws Exception {
            mockMvc.perform(get("/api/drugs/not-a-number"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Invalid value for parameter 'id': not-a-number")));
        }
    }

    @Nested
    @DisplayName("Drug not found")
    class DrugNotFoundTest {

        @Test
        void shouldReturn404WhenDrugIsNotFound() throws Exception {
            int nonExistentId = 999999;
            given(drugService.getDrugById(nonExistentId))
                    .willThrow(new DrugNotFoundException("Drug with ID %d not found".formatted(nonExistentId)));

            mockMvc.perform(get("/api/drugs/999999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Drug with ID 999999 not found"));
        }
    }

    @Nested
    @DisplayName("Illegal argument")
    class IllegalArgumentTest {

        @Test
        @DisplayName("shouldReturn400WhenServiceThrowsIllegalArgument")
        void shouldReturn400WhenServiceThrowsIllegalArgument() throws Exception {
            given(drugService.getDrugById(1)).willThrow(new IllegalArgumentException("Bad argument"));

            mockMvc.perform(get("/api/drugs/1"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("Bad argument"));
        }
    }

    @Nested
    @DisplayName("Invalid sort field handling")
    class InvalidSortFieldIntegrationTest {
        @Test
        void shouldReturn400WhenSortFieldIsInvalid() throws Exception {
            given(drugService.searchDrugs(
                    nullable(String.class),
                    nullable(String.class),
                    any(), any(), any(), any()
            )).willThrow(new InvalidSortFieldException("unknown"));

            mockMvc.perform(get("/api/drugs/search")
                            .param("sort", "unknown,asc")
                            .param("name", "Ibuprofen"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("Unknown sort field: unknown"));
        }
    }

    @Nested
    @DisplayName("HandlerMethodValidationException handling")
    class HandlerMethodValidationIntegrationTest {
        @Test
        void shouldReturn400WhenMethodParamViolatesConstraints() throws Exception {
            mockMvc.perform(get("/api/drugs/search")
                            .param("expirationUntilMonth", "0")) // invalid month
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("expirationUntilMonth")));
        }

        @Test
        void shouldIncludeParamNamesAndMessages_whenMultipleParamsInvalid() throws Exception {
            mockMvc.perform(get("/api/drugs/search")
                            .param("expirationUntilMonth", "0")   // invalid month
                            .param("expirationUntilYear", "1999")) // invalid year (below min)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("expirationUntilMonth")))
                    .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("expirationUntilYear")))
                    // default Bean Validation messages usually include the word "must"
                    .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("must")));
        }
    }

    @Nested
    @DisplayName("Missing request parameter handling")
    class MissingRequestParamIntegrationTest {
        @Test
        void shouldReturn200WhenSizeOmitted_exportUsesDefault() throws Exception {
            // stub search to return empty page (controller still exports header/body)
            given(drugService.searchDrugs(
                    nullable(String.class), nullable(String.class),
                    any(), any(), any(), any()
            )).willReturn(Page.empty());

            // stub PDF exporter to return some bytes
            byte[] bytes = new byte[]{1, 2, 3};
            given(pdfExportService.generatePdf(any())).willReturn(bytes);

            mockMvc.perform(get("/api/drugs/export/pdf"))
                    .andExpect(status().isOk())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_PDF));
        }
    }

    @Nested
    @DisplayName("GlobalExceptionHandler unit tests")
    class GlobalHandlerUnitTest {
        @Test
        void shouldBuild400ResponseForMissingParam() {
            GlobalExceptionHandler handler = new GlobalExceptionHandler();
            var ex = new org.springframework.web.bind.MissingServletRequestParameterException("size", "Integer");

            var response = handler.handleMissingParam(ex);

            org.assertj.core.api.Assertions.assertThat(response.getStatusCode().value()).isEqualTo(400);
            org.assertj.core.api.Assertions.assertThat(Objects.requireNonNull(response.getBody()).message()).isEqualTo
                    ("Missing required request parameter: size");
        }

    }

    @Nested
    @DisplayName("EmailSendingException handling")
    class EmailSendingExceptionIntegrationTest {
        @Test
        void shouldReturn500WhenEmailSendingFails() throws Exception {
            // Arrange: when alert endpoint triggers service, throw EmailSendingException
            willThrow(new EmailSendingException("boom")).given(drugService).sendDefaultExpiryAlertEmailsForCurrentUser();

            mockMvc.perform(post("/api/email/alert"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status")
                            .value(500))
                    .andExpect(jsonPath("$.message")
                            .value("Failed to send expiry alert email. Please try again later."));
        }
    }
}
