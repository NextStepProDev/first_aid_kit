package com.firstaid.integration;

import com.firstaid.infrastructure.email.EmailService;
import com.firstaid.infrastructure.pdf.PdfExportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.alert.recipientEmail=test@example.com"
})
class SmokeApiTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    PdfExportService pdfExportService;

    @MockitoBean
    EmailService emailService;

    @Value("${app.alert.recipientEmail:}")
    private String alertRecipientEmail;

    @Test
    @DisplayName("GET /api/drugs/search -> 200 (bez filtrów, default size)")
    void search_shouldReturn200_withoutFilters() throws Exception {
        mvc.perform(get("/api/drugs/search"))
           .andExpect(status().isOk())
           .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("GET /api/drugs/search -> 400 gdy size > 100")
    void search_shouldReject_whenSizeOver100() throws Exception {
        mvc.perform(get("/api/drugs/search")
                .param("size", "101"))
           .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/drugs/export/pdf -> 400 gdy size > 100")
    void exportPdf_shouldReject_whenSizeOver100() throws Exception {
        mvc.perform(get("/api/drugs/export/pdf")
                .param("size", "101"))
           .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/drugs/forms -> 200 i JSON")
    void forms_shouldReturn200() throws Exception {
        mvc.perform(get("/api/drugs/forms"))
           .andExpect(status().isOk())
           .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("GET /api/drugs/forms/dictionary -> 200 i JSON")
    void formsDictionary_shouldReturn200() throws Exception {
        mvc.perform(get("/api/drugs/forms/dictionary"))
           .andExpect(status().isOk())
           .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("GET /api/drugs/{id} -> 404 dla nieistniejącego")
    void getById_shouldReturn404_whenNotFound() throws Exception {
        mvc.perform(get("/api/drugs/{id}", 999_999))
           .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/drugs/search -> 200 dla sortowania po relacji (drugForm.name)")
    void search_shouldSupportSortByNestedField() throws Exception {
        mvc.perform(get("/api/drugs/search")
                .param("sort", "drugForm.name,asc")
                .param("size", "5"))
           .andExpect(status().isOk());
    }
}