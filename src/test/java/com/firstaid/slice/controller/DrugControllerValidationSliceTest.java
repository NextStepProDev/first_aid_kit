package com.firstaid.slice.controller;

import com.firstaid.config.NoSecurityConfig;
import com.firstaid.controller.DrugController;
import com.firstaid.infrastructure.pdf.PdfExportService;
import com.firstaid.service.DrugService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.stream.Stream;

import static com.firstaid.util.BuildJson.buildJson;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DrugController.class)
@Import(NoSecurityConfig.class)
public class DrugControllerValidationSliceTest {

    @Autowired
    @SuppressWarnings("unused")
    private MockMvc mockMvc;

    @MockitoBean
    @SuppressWarnings("unused")
    private DrugService drugService;

    @MockitoBean
    @SuppressWarnings("unused")
    private PdfExportService pdfExportService;

    static Stream<Arguments> invalidMonthProvider() {
        int currentMonth = OffsetDateTime.now().getMonthValue();
        return Stream.of(
                Arguments.of(true, currentMonth),   // valid: current month with current year
                Arguments.of(true, 12),             // valid: December
                Arguments.of(false, 0),
                Arguments.of(false, null),
                Arguments.of(false, -1),
                Arguments.of(false, currentMonth - 1)
        );
    }

    static Stream<Arguments> invalidYearProvider() {
        return Stream.of(
                Arguments.of(true, OffsetDateTime.now().getYear() + 1),
                Arguments.of(false, 2023),
                Arguments.of(false, 0),
                Arguments.of(false, null)
        );
    }

    static Stream<Arguments> invalidNameProvider() {
        return Stream.of(
                Arguments.of(true, "Ibuprofen"),
                Arguments.of(true, "Altacet"),
                Arguments.of(false, ""),
                Arguments.of(false, null)
        );
    }

    static Stream<Arguments> formProvider() {
        return Stream.of(
                Arguments.of(true, "PILLS"),
                Arguments.of(true, "SYRUP"),
                Arguments.of(false, "INVALID_FORM"),
                Arguments.of(false, "")
        );
    }

    static Stream<Arguments> invalidDescriptionProvider() {
        return Stream.of(
                Arguments.of(true, "Painkiller for fever and inflammation"),
                Arguments.of(false, ""),
                Arguments.of(false, null)
        );
    }

    static Stream<Arguments> formBlankOrNullProvider() {
        return Stream.of(
                Arguments.of(false, null),
                Arguments.of(false, ""),
                Arguments.of(false, " ")
        );
    }

    @ParameterizedTest
    @MethodSource("invalidMonthProvider")
    @DisplayName("Should return 201 for valid month and 400 for invalid month")
    void shouldReturnBadRequestForInvalidMonth(Boolean isValid, Integer month) throws Exception {
        String json = buildJson("Ibuprofen", "PILLS", 2025, month, "lek przeciwbólowy");
        mockMvc.perform(
                        post("/api/drugs")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(isValid ? status().isCreated() : status().isBadRequest());
    }

    @ParameterizedTest
    @MethodSource("invalidYearProvider")
    @DisplayName("Should return 201 for valid year and 400 for invalid year")
    void shouldReturnBadRequestForInvalidYear(Boolean isValid, Integer year) throws Exception {
        String json = buildJson("Ibuprofen", "PILLS", year, 12, "lek przeciwbólowy");
        mockMvc.perform(
                        post("/api/drugs")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(isValid ? status().isCreated() : status().isBadRequest());
    }

    @ParameterizedTest
    @MethodSource("invalidNameProvider")
    @DisplayName("Should return 201 for valid name and 400 for invalid name")
    void shouldReturnBadRequestForInvalidNames(Boolean isValid, String name) throws Exception {
        String json = buildJson(name, "PILLS", 2025, 12, "lek przeciwbólowy");
        mockMvc.perform(
                        post("/api/drugs")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(isValid ? status().isCreated() : status().isBadRequest());

    }

    @ParameterizedTest
    @MethodSource("formProvider")
    @DisplayName("Should return 201 for valid form and 400 for invalid form")
        // slice test, czyli testujemy tylko kontroler
    void shouldReturnBadRequestForInvalidForm(boolean isValid, String form) throws Exception {
        String json = buildJson("Ibuprofen", form, 2025, 12, "pain relief medication");
        mockMvc.perform(
                        post("/api/drugs")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(isValid ? status().isCreated() : status().isBadRequest());
    }

    @ParameterizedTest
    @MethodSource("invalidDescriptionProvider")
    @DisplayName("Should return 201 for valid description and 400 for invalid description")
    void shouldReturnBadRequestForInvalidDescription(Boolean isValid, String description) throws Exception {
        String json = buildJson("Ibuprofen", "PILLS", 2025, 12, description);
        mockMvc.perform(
                        post("/api/drugs")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(isValid ? status().isCreated() : status().isBadRequest());
    }

    @ParameterizedTest
    @MethodSource("formBlankOrNullProvider")
    @DisplayName("Should return 400 for null or blank form values")
    void shouldReturnBadRequestForBlankOrNullForm(boolean isValid, String form) throws Exception {
        String json = buildJson("Ibuprofen", form, 2025, 12, "Painkiller for fever");
        mockMvc.perform(
                        post("/api/drugs")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(isValid ? status().isCreated() : status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 201 for valid request")
    void shouldReturn201ForValidRequest() throws Exception {
        String json = buildJson("Ibuprofen", "PILLS", 2025, 12,
                "pain relief medication");
        mockMvc.perform(post("/api/drugs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());
    }
}