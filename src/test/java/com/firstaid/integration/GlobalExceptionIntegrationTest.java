//package com.firstaid.integration;
//
//import com.firstaid.config.NoSecurityConfig;
//import com.firstaid.controller.DrugController;
//import com.firstaid.controller.dto.SortDirectionDTO;
//import com.firstaid.controller.exception.DrugNotFoundException;
//import com.firstaid.controller.exception.GlobalExceptionHandler;
//import com.firstaid.controller.exception.InvalidSortFieldException;
//import com.firstaid.infrastructure.pdf.PdfExportService;
//import com.firstaid.service.DrugService;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
//import org.springframework.context.annotation.Import;
//import org.springframework.http.MediaType;
//import org.springframework.test.context.bean.override.mockito.MockitoBean;
//import org.springframework.test.web.servlet.MockMvc;
//
//import java.time.OffsetDateTime;
//import java.util.Map;
//
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.BDDMockito.given;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//@WebMvcTest(controllers = DrugController.class)
//@Import({GlobalExceptionHandler.class, NoSecurityConfig.class})
//class GlobalExceptionIntegrationTest {
//
//    static int CURRENT_YEAR = OffsetDateTime.now().getYear();
//    static int NEXT_MONTH = OffsetDateTime.now().getMonthValue() + 1;
//
//    @Autowired
//    @SuppressWarnings("unused")
//    private MockMvc mockMvc;
//
//    @Autowired
//    @SuppressWarnings("unused")
//    private ObjectMapper objectMapper;
//
//    @MockitoBean
//    @SuppressWarnings("unused")
//    private DrugService drugService;
//
//    @MockitoBean
//    @SuppressWarnings("unused")
//    private PdfExportService pdfExportService;
//
//    @Nested
//    @DisplayName("Validation errors (@Valid DTO)")
//    class ValidationErrorsTest {
//
//        @Test
//        void shouldReturn400WhenExpirationYearIsInThePast() throws Exception {
//            Map<String, Object> invalidDto = Map.of(
//                    "name", "Ibuprofen",
//                    "form", "PILLS",
//                    "description", "Painkiller",
//                    "expirationYear", 1000,  // za mały
//                    "expirationMonth", NEXT_MONTH
//            );
//
//            mockMvc.perform(post("/api/drugs")
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(invalidDto)))
//                    .andExpect(status().isBadRequest())
//                    .andExpect(jsonPath("$.errors[*].field").value(org.hamcrest.Matchers.hasItem("expirationYear")));
//        }
//
//        @Test
//        void shouldReturn400WhenDtoValidationFails() throws Exception {
//            Map<String, Object> invalidDto = Map.of(
//                    "name", "",
//                    "form", "INVALID_FORM",
//                    "expirationYear", CURRENT_YEAR,
//                    "expirationMonth", NEXT_MONTH
//            );
//
//            mockMvc.perform(post("/api/drugs")
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(invalidDto)))
//                    .andExpect(status().isBadRequest())
//                    .andExpect(jsonPath("$.status").value(400))
//                    .andExpect(jsonPath("$.errors").isNotEmpty())
//                    .andExpect(jsonPath("$.message").value("Validation failed"))
//                    .andExpect(jsonPath("$.errors").isArray())
//                    .andExpect(jsonPath("$.errors").isNotEmpty());
//        }
//
//        @Test
//        void shouldReturn400WhenDescriptionIsBlank() throws Exception {
//            Map<String, Object> invalidDto = Map.of(
//                    "name", "Ibuprofen",
//                    "form", "PILLS",
//                    "description", "",  // <---
//                    "expirationYear", CURRENT_YEAR,
//                    "expirationMonth", NEXT_MONTH
//            );
//
//            mockMvc.perform(post("/api/drugs")
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(invalidDto)))
//                    .andExpect(status().isBadRequest())
//                    .andExpect(jsonPath("$.message").value("Validation failed"))
//                    .andExpect(jsonPath("$.errors").isArray())
//                    .andExpect(jsonPath("$.errors[*].field").value(org.hamcrest.Matchers.hasItem("description")));
//        }
//
//        @Test
//        void shouldReturn400WhenExpirationMonthIsOutOfRange() throws Exception {
//            Map<String, Object> invalidDto = Map.of(
//                    "name", "Ibuprofen",
//                    "form", "PILLS",
//                    "description", "Painkiller",
//                    "expirationYear", CURRENT_YEAR,
//                    "expirationMonth", 15
//            );
//
//            mockMvc.perform(post("/api/drugs")
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(invalidDto)))
//                    .andExpect(status().isBadRequest())
//                    .andExpect(jsonPath("$.errors[*].field").value(org.hamcrest.Matchers.hasItem("expirationMonth")));
//        }
//    }
//
//    @Nested
//    @DisplayName("Malformed JSON")
//    class MalformedJsonTest {
//
//        @Test
//        void shouldReturn400ForUnreadableJson() throws Exception {
//            // given
//            String malformedJson = "{ \"name\": \"Paracetamol\", \"form\": \"PILLS\" "; // Missing closing brace
//
//            // when & then
//            mockMvc.perform(post("/api/drugs")
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(malformedJson))
//                    .andExpect(status().isBadRequest())
//                    .andExpect(jsonPath("$.error").value("Malformed JSON"))
//                    .andExpect(jsonPath("$.message").value("Request body is invalid or unreadable"));
//        }
//    }
//
//    @Nested
//    @DisplayName("Invalid Request Parameters")
//    class ConstraintViolationTest {
//
//        @Test
//        void shouldReturn400ForInvalidRequestParam() throws Exception {
//            // given
//            String invalidField = "unknownField";
//            given(drugService.getAllSorted(invalidField, SortDirectionDTO.ASC))
//                    .willThrow(new InvalidSortFieldException(invalidField));
//
//            // when & then
//            mockMvc.perform(get("/api/drugs/sorted")
//                            .param("sortBy", "unknownField")
//                            .param("direction", "asc"))
//                    .andExpect(status().isBadRequest())
//                    .andExpect(jsonPath("$.message")
//                            .value("Invalid value for parameter 'sortBy': " + invalidField));
//        }
//
//        @Test
//        void shouldReturn400ForInvalidRequestParamTypeYear() throws Exception {
//            // when & then
//            mockMvc.perform(get("/api/drugs//expiration-until")
//                            .param("year", "abc")
//                            .param("month", "12"))
//                    .andExpect(status().isBadRequest())
//                    .andExpect(jsonPath("$.status").value(400))
//                    .andExpect(jsonPath("$.message").value("Invalid value for parameter 'year': abc"));
//        }
//
//        @Test
//        void shouldReturn400WhenSortByParamIsEmpty() throws Exception {
//            // given
//            given(drugService.getAllSorted(anyString(), any()))
//                    .willThrow(new InvalidSortFieldException(""));
//
//            // when & then
//            mockMvc.perform(get("/api/drugs/sorted")
//                            .param("sortBy", "")
//                            .param("direction", "asc"))
//                    .andExpect(status().isBadRequest())
//                    .andExpect(jsonPath("$.message").value("Invalid value for parameter 'sortBy': "));
//        }
//
//        @Test
//        void shouldReturn400ForInvalidRequestParamTypeMonth() throws Exception {
//            // when & then
//            mockMvc.perform(get("/api/drugs/expiration-until")
//                            .param("year", String.valueOf(CURRENT_YEAR))
//                            .param("month", "14"))
//                    .andExpect(status().isBadRequest())
//                    .andExpect(jsonPath("$.status").value(400))
//                    .andExpect(jsonPath("$.message").value("400 BAD_REQUEST \"Validation failure\""));
//        }
//    }
//
//    @Nested
//    @DisplayName("Drug not found")
//    class DrugNotFoundTest {
//
//        @Test
//        void shouldReturn404WhenDrugIsNotFound() throws Exception {
//            int nonExistentId = 999999;
//            given(drugService.getDrugById(nonExistentId))
//                    .willThrow(new DrugNotFoundException("Drug with ID %d not found".formatted(nonExistentId)));
//
//            mockMvc.perform(get("/api/drugs/999999"))
//                    .andExpect(status().isNotFound())
//                    .andExpect(jsonPath("$.message").value("Drug with ID 999999 not found"));
//        }
//    }
//
//    @Nested
//    @DisplayName("Illegal argument")
//    class IllegalArgumentTest {
//        @Test
//        void shouldReturn400ForIllegalArgument() throws Exception {
//            // given
//            String invalidDrugName = "Invalid Drug Name!@#";
//            given(drugService.getDrugsByName(invalidDrugName))
//                    .willThrow(new IllegalArgumentException("Invalid drug name: %s".formatted(invalidDrugName)));
//
//            // when & then
//            mockMvc.perform(get("/api/drugs/by-name")
//                            .param("name", invalidDrugName))
//                    .andExpect(status().isBadRequest())
//                    .andExpect(jsonPath("$.message").value("Invalid drug name: Invalid Drug Name!@#"));
//        }
//    }
//}
//
////    Email sending failure is already tested in DrugControllerIntegrationTest