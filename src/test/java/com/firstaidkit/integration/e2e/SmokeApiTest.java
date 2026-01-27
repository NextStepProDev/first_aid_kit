package com.firstaidkit.integration.e2e;

import com.firstaidkit.controller.dto.drug.DrugFormDTO;
import com.firstaidkit.infrastructure.database.entity.DrugEntity;
import com.firstaidkit.infrastructure.database.entity.DrugFormEntity;
import com.firstaidkit.infrastructure.database.repository.DrugFormRepository;
import com.firstaidkit.infrastructure.pdf.PdfExportService;
import com.firstaidkit.infrastructure.util.DateUtils;
import com.firstaidkit.integration.base.AbstractIntegrationTest;
import com.firstaidkit.service.DrugFormService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import static com.firstaidkit.util.BuildJson.buildJson;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc(addFilters = false)
class SmokeApiTest extends AbstractIntegrationTest {

    private static final int INVALID_PAGE_SIZE = 1001;
    private static final int TEST_PAGE_SIZE = 5;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final String FORM_PILLS = "PILLS";
    private static final String INVALID_FORM = "INVALID_FORM";
    private static final int NONEXISTENT_DRUG_ID = 999_999;
    private static final String NAME_FILTER = "asp";
    private static final String PDF_FILENAME = "drugs_list.pdf";
    private static final int FUTURE_YEAR = OffsetDateTime.now().getYear() + 1;
    private static final int INVALID_YEAR = 2023;
    private static final int VALID_YEAR = 2026;
    private static final int DEFAULT_MONTH = 12;
    private static final int INVALID_MONTH_HIGH = 13;
    private static final int INVALID_MONTH_LOW = 0;
    private static final byte[] DUMMY_PDF_BYTES = "dummy-pdf".getBytes();

    @Autowired
    MockMvc mvc;

    @MockitoBean
    PdfExportService pdfExportService;

    @Autowired
    DrugFormRepository drugFormRepository;

    @Autowired
    DrugFormService drugFormService;

    // Helper Methods
    private void expectJsonOkResponse(ResultActions result) throws Exception {
        result.andExpect(status().isOk())
              .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    private void expectPdfResponse(ResultActions result) throws Exception {
        result.andExpect(status().isOk())
              .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PDF))
              .andExpect(header().string("Content-Disposition", containsString("inline")))
              .andExpect(header().string("Content-Disposition", containsString(PDF_FILENAME)));
    }

    private DrugEntity createTestDrug(String name, String description) {
        DrugFormEntity form = drugFormService.resolve(DrugFormDTO.PILLS);
        return drugRepository.saveAndFlush(DrugEntity.builder()
                .drugName(name)
                .drugForm(form)
                .owner(getTestUser())
                .expirationDate(DateUtils.buildExpirationDate(FUTURE_YEAR, DEFAULT_MONTH))
                .drugDescription(description)
                .alertSent(false)
                .build());
    }

    @Nested
    @DisplayName("Search API")
    class SearchTests {
        @Test
        @DisplayName("GET /api/drugs/search -> 200 (no filters, default size)")
        void search_shouldReturn200_withoutFilters() throws Exception {
            expectJsonOkResponse(mvc.perform(get("/api/drugs/search")));
        }

        @Test
        @DisplayName("GET /api/drugs/search -> 400 when size > 100")
        void search_shouldReject_whenSizeOver100() throws Exception {
            mvc.perform(get("/api/drugs/search")
                            .param("size", String.valueOf(INVALID_PAGE_SIZE)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("GET /api/drugs/search -> 200 for sorting by relation (drugForm.name)")
        void search_shouldSupportSortByNestedField() throws Exception {
            mvc.perform(get("/api/drugs/search")
                            .param("sort", "drugForm.name,asc")
                            .param("size", String.valueOf(TEST_PAGE_SIZE)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /api/drugs/search -> 200 with name filter")
        void search_shouldReturn200_withNameFilter() throws Exception {
            expectJsonOkResponse(mvc.perform(get("/api/drugs/search").param("name", NAME_FILTER)));
        }

        @Test
        @DisplayName("GET /api/drugs/search -> 200 with form filter (valid)")
        void search_shouldReturn200_withValidFormFilter() throws Exception {
            expectJsonOkResponse(mvc.perform(get("/api/drugs/search").param("form", FORM_PILLS)));
        }

        @Test
        @DisplayName("GET /api/drugs/search -> 400 with form filter (invalid)")
        void search_shouldReturn400_withInvalidFormFilter() throws Exception {
            mvc.perform(get("/api/drugs/search").param("form", INVALID_FORM))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("GET /api/drugs/search -> 200 with expired=true")
        void search_shouldReturn200_withExpiredTrue() throws Exception {
            expectJsonOkResponse(mvc.perform(get("/api/drugs/search").param("expired", "true")));
        }

        @Test
        @DisplayName("GET /api/drugs/search -> 200 with expired=false")
        void search_shouldReturn200_withExpiredFalse() throws Exception {
            expectJsonOkResponse(mvc.perform(get("/api/drugs/search").param("expired", "false")));
        }

        @Test
        @DisplayName("GET /api/drugs/search -> 200 with year-only (defaults month=12)")
        void search_shouldReturn200_withOnlyYear() throws Exception {
            expectJsonOkResponse(mvc.perform(get("/api/drugs/search")
                    .param("expirationUntilYear", String.valueOf(VALID_YEAR))));
        }

        @Test
        @DisplayName("GET /api/drugs/search -> 200 with month-only (defaults year=current)")
        void search_shouldReturn200_withOnlyMonth() throws Exception {
            expectJsonOkResponse(mvc.perform(get("/api/drugs/search")
                    .param("expirationUntilMonth", String.valueOf(DEFAULT_MONTH))));
        }

        @Test
        @DisplayName("GET /api/drugs/search -> 400 when year < 2024")
        void search_shouldReturn400_whenYearBelowMin() throws Exception {
            mvc.perform(get("/api/drugs/search")
                            .param("expirationUntilYear", String.valueOf(INVALID_YEAR)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("GET /api/drugs/search -> 400 when month > 12")
        void search_shouldReturn400_whenMonthAboveMax() throws Exception {
            mvc.perform(get("/api/drugs/search")
                            .param("expirationUntilMonth", String.valueOf(INVALID_MONTH_HIGH)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("GET /api/drugs/search -> 400 with invalid sort field")
        void search_shouldReturn400_withInvalidSortField() throws Exception {
            mvc.perform(get("/api/drugs/search").param("sort", "nonExistingField,asc"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Forms API")
    class FormsTests {
        @Test
        @DisplayName("GET /api/drugs/forms -> 200 and JSON")
        void forms_shouldReturn200() throws Exception {
            expectJsonOkResponse(mvc.perform(get("/api/drugs/forms")));
        }

        @Test
        @DisplayName("GET /api/drugs/forms/dictionary -> 200 and JSON")
        void formsDictionary_shouldReturn200() throws Exception {
            expectJsonOkResponse(mvc.perform(get("/api/drugs/forms/dictionary")));
        }
    }

    @Nested
    @DisplayName("Drug by ID API")
    class DrugByIdTests {
        @Test
        @DisplayName("GET /api/drugs/{id} -> 404 when not found")
        void getById_shouldReturn404_whenNotFound() throws Exception {
            mvc.perform(get("/api/drugs/{id}", NONEXISTENT_DRUG_ID))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("GET /api/drugs/{id} -> 200 when entity exists (seeded)")
        @Transactional
        void getById_shouldReturn200_whenExists_seeded() throws Exception {
            DrugEntity saved = createTestDrug("Seeded-Drug", "seeded");

            expectJsonOkResponse(mvc.perform(get("/api/drugs/{id}", saved.getDrugId())));
        }
    }

    @Nested
    @DisplayName("PDF Export API")
    class PdfExportTests {
        @BeforeEach
        void setupPdfMock() {
            given(pdfExportService.generatePdf(anyList())).willReturn(DUMMY_PDF_BYTES);
        }

        @Test
        @DisplayName("GET /api/drugs/export/pdf -> 400 when size > 1000")
        void exportPdf_shouldReject_whenSizeOver100() throws Exception {
            mvc.perform(get("/api/drugs/export/pdf")
                            .param("size", String.valueOf(INVALID_PAGE_SIZE)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("GET /api/drugs/export/pdf -> 200 for valid export")
        void exportPdf_shouldReturn200_whenValidExport() throws Exception {
            ResultActions result = mvc.perform(get("/api/drugs/export/pdf")
                            .param("page", "0")
                            .param("size", String.valueOf(DEFAULT_PAGE_SIZE))
                            .param("sort", "drugName,asc"));

            expectPdfResponse(result);
            result.andExpect(r -> {
                byte[] bytes = r.getResponse().getContentAsByteArray();
                assertThat(bytes).isNotEmpty();
            });
        }

        @Test
        @DisplayName("GET /api/drugs/export/pdf -> 200 with filters applied")
        void exportPdf_shouldReturn200_withFilters() throws Exception {
            ResultActions result = mvc.perform(get("/api/drugs/export/pdf")
                            .param("name", NAME_FILTER)
                            .param("form", FORM_PILLS)
                            .param("expired", "false")
                            .param("page", "0")
                            .param("size", String.valueOf(DEFAULT_PAGE_SIZE))
                            .param("sort", "drugName,asc"));

            expectPdfResponse(result);
        }
    }

    @Nested
    @DisplayName("Statistics API")
    class StatisticsTests {
        @Test
        @DisplayName("GET /api/drugs/statistics -> 200")
        void getStatistics_shouldReturn200() throws Exception {
            expectJsonOkResponse(mvc.perform(get("/api/drugs/statistics")));
        }

        @Test
        @DisplayName("GET /api/drugs/statistics -> 200 and has expected JSON keys")
        void getStatistics_shouldContainExpectedKeys() throws Exception {
            mvc.perform(get("/api/drugs/statistics"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.totalDrugs").exists())
                    .andExpect(jsonPath("$.expiredDrugs").exists())
                    .andExpect(jsonPath("$.activeDrugs").exists())
                    .andExpect(jsonPath("$.alertSentCount").exists())
                    .andExpect(jsonPath("$.drugsByForm").exists());
        }
    }

    @Nested
    @DisplayName("Legacy Add Endpoints (GET)")
    class AddDrugLegacyTests {
        @Test
        @DisplayName("GET /api/drugs/add -> 400 for bad expiration date")
        void addDrug_shouldReturn400_whenInvalidData() throws Exception {
            mvc.perform(get("/api/drugs/add")
                            .param("drugName", "Aspirin")
                            .param("drugForm", FORM_PILLS)
                            .param("expirationDate", "2023-01-01"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("GET /api/drugs/add -> 400 for invalid form")
        void addDrug_shouldReturn400_whenInvalidForm() throws Exception {
            mvc.perform(get("/api/drugs/add")
                            .param("drugName", "Aspirin")
                            .param("drugForm", INVALID_FORM)
                            .param("expirationDate", String.valueOf(OffsetDateTime.now())))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Create Drug (POST)")
    class AddDrugPostTests {
        @Test
        @DisplayName("POST /api/drugs -> 201 for valid payload")
        void addDrug_shouldReturn201_whenValidData() throws Exception {
            String json = buildJson("Aspirin", FORM_PILLS,
                    FUTURE_YEAR,
                    LocalDate.now().getMonthValue(),
                    "Painkiller for fever and inflammation");

            mvc.perform(post("/api/drugs")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("POST /api/drugs -> 400 when form is invalid")
        void addDrug_shouldReturn400_whenFormInvalid() throws Exception {
            String json = buildJson("Aspirin", INVALID_FORM,
                    FUTURE_YEAR,
                    LocalDate.now().getMonthValue(),
                    "Desc");

            mvc.perform(post("/api/drugs")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST /api/drugs -> 400 when required fields are missing")
        void addDrug_shouldReturn400_whenFieldsMissing() throws Exception {
            String json = """
                    {
                      "name": "",
                      "form": "",
                      "expirationYear": null,
                      "expirationMonth": null,
                      "description": ""
                    }""";

            mvc.perform(post("/api/drugs")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Routing & Error Handling")
    class RoutingAndErrorsTests {
        @Test
        @DisplayName("GET /api/drugs/{id} -> 400 when id is not a number")
        void getById_shouldReturn400_whenIdIsNonNumeric() throws Exception {
            mvc.perform(get("/api/drugs/{id}", "abc"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("GET /api/drugs/unknown-endpoint/extra -> 404 Not Found")
        void unknownEndpoint_shouldReturn404() throws Exception {
            mvc.perform(get("/api/drugs/unknown-endpoint/extra"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("GET /api/drugs/unknown-endpoint -> 400 Bad Request (parsed as ID)")
        void unrecognizedEndpoint_shouldReturn400() throws Exception {
            mvc.perform(get("/api/drugs/unknown-endpoint"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("GET /api/drugs/search -> 200 with multiple sort fields")
        void search_shouldReturn200_withMultipleSortFields() throws Exception {
            expectJsonOkResponse(mvc.perform(get("/api/drugs/search")
                            .param("sort", "drugName,desc")
                            .param("sort", "drugForm.name,asc")
                            .param("size", String.valueOf(TEST_PAGE_SIZE))));
        }

        @Test
        @DisplayName("GET /api/drugs/search -> 400 when month < 1")
        void search_shouldReturn400_whenMonthBelowMin() throws Exception {
            mvc.perform(get("/api/drugs/search")
                            .param("expirationUntilMonth", String.valueOf(INVALID_MONTH_LOW)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Update Drug (PUT)")
    class UpdateDrugPutTests {
        @Test
        @DisplayName("PUT /api/drugs/{id} -> 204 when entity exists (seeded)")
        @Transactional
        void update_shouldReturn204_whenEntityExists_seeded() throws Exception {
            DrugFormEntity form = drugFormRepository.findAll().stream().findFirst().orElseThrow();
            DrugEntity saved = createTestDrug("Update-Drug", "update test");

            String payload = """
                    {
                        "name": "Updated Name",
                        "form": "%s",
                        "expirationYear": %d,
                        "expirationMonth": %d,
                        "description": "Updated description"
                    }
                    """.formatted(form.getName(), FUTURE_YEAR + 1, DEFAULT_MONTH);

            mvc.perform(put("/api/drugs/{id}", saved.getDrugId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("PUT /api/drugs/{id} -> 404 when entity not found")
        void update_shouldReturn404_whenNotFound() throws Exception {
            String payload = """
                    {
                        "name": "Updated Name",
                        "form": "%s",
                        "expirationYear": %d,
                        "expirationMonth": %d,
                        "description": "Updated description"
                    }
                    """.formatted(FORM_PILLS, FUTURE_YEAR + 1, DEFAULT_MONTH);

            mvc.perform(put("/api/drugs/{id}", NONEXISTENT_DRUG_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Delete Drug (DELETE)")
    class DeleteDrugTests {
        @Test
        @DisplayName("DELETE /api/drugs/{id} -> 204 when entity exists (seeded)")
        @Transactional
        void delete_shouldReturn204_whenEntityExists_seeded() throws Exception {
            DrugEntity saved = createTestDrug("Delete-Drug", "delete test");

            mvc.perform(delete("/api/drugs/{id}", saved.getDrugId()))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("DELETE /api/drugs/{id} -> 404 when entity not found")
        void delete_shouldReturn404_whenNotFound() throws Exception {
            mvc.perform(delete("/api/drugs/{id}", NONEXISTENT_DRUG_ID))
                    .andExpect(status().isNotFound());
        }
    }
}
