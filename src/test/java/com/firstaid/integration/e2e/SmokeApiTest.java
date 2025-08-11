package com.firstaid.integration.e2e;

import com.firstaid.controller.dto.DrugFormDTO;
import com.firstaid.infrastructure.database.entity.DrugEntity;
import com.firstaid.infrastructure.database.entity.DrugFormEntity;
import com.firstaid.infrastructure.database.repository.DrugFormRepository;
import com.firstaid.infrastructure.database.repository.DrugRepository;
import com.firstaid.infrastructure.pdf.PdfExportService;
import com.firstaid.infrastructure.util.DateUtils;
import com.firstaid.service.DrugFormService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import static com.firstaid.util.BuildJson.buildJson;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class SmokeApiTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    PdfExportService pdfExportService;

    @Value("${app.alert.recipientEmail:}")
    @SuppressWarnings("unused")
    private String alertRecipientEmail;

    @Autowired
    DrugRepository drugRepository;

    @Autowired
    DrugFormRepository drugFormRepository;

    @Autowired
    DrugFormService drugFormService;

    @Nested
    @DisplayName("Search API")
    class SearchTests {
        @Test
        @DisplayName("GET /api/drugs/search -> 200 (no filters, default size)")
        void search_shouldReturn200_withoutFilters() throws Exception {
            mvc.perform(get("/api/drugs/search"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("GET /api/drugs/search -> 400 when size > 100")
        void search_shouldReject_whenSizeOver100() throws Exception {
            mvc.perform(get("/api/drugs/search")
                            .param("size", "101"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("GET /api/drugs/search -> 200 for sorting by relation (drugForm.name)")
        void search_shouldSupportSortByNestedField() throws Exception {
            mvc.perform(get("/api/drugs/search")
                            .param("sort", "drugForm.name,asc")
                            .param("size", "5"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /api/drugs/search -> 200 with name filter")
        void search_shouldReturn200_withNameFilter() throws Exception {
            mvc.perform(get("/api/drugs/search").param("name", "asp"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("GET /api/drugs/search -> 200 with form filter (valid)")
        void search_shouldReturn200_withValidFormFilter() throws Exception {
            mvc.perform(get("/api/drugs/search").param("form", "PILLS"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("GET /api/drugs/search -> 400 with form filter (invalid)")
        void search_shouldReturn400_withInvalidFormFilter() throws Exception {
            mvc.perform(get("/api/drugs/search").param("form", "INVALID_FORM"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("GET /api/drugs/search -> 200 with expired=true")
        void search_shouldReturn200_withExpiredTrue() throws Exception {
            mvc.perform(get("/api/drugs/search").param("expired", "true"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("GET /api/drugs/search -> 200 with expired=false")
        void search_shouldReturn200_withExpiredFalse() throws Exception {
            mvc.perform(get("/api/drugs/search").param("expired", "false"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("GET /api/drugs/search -> 200 with year-only (defaults month=12)")
        void search_shouldReturn200_withOnlyYear() throws Exception {
            mvc.perform(get("/api/drugs/search").param("expirationUntilYear", "2026"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("GET /api/drugs/search -> 200 with month-only (defaults year=current)")
        void search_shouldReturn200_withOnlyMonth() throws Exception {
            mvc.perform(get("/api/drugs/search").param("expirationUntilMonth", "12"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("GET /api/drugs/search -> 400 when year < 2024")
        void search_shouldReturn400_whenYearBelowMin() throws Exception {
            mvc.perform(get("/api/drugs/search").param("expirationUntilYear", "2023"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("GET /api/drugs/search -> 400 when month > 12")
        void search_shouldReturn400_whenMonthAboveMax() throws Exception {
            mvc.perform(get("/api/drugs/search").param("expirationUntilMonth", "13"))
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
            mvc.perform(get("/api/drugs/forms"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("GET /api/drugs/forms/dictionary -> 200 and JSON")
        void formsDictionary_shouldReturn200() throws Exception {
            mvc.perform(get("/api/drugs/forms/dictionary"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        }
    }

    @Nested
    @DisplayName("Drug by ID API")
    class DrugByIdTests {
        @Test
        @DisplayName("GET /api/drugs/{id} -> 404 when not found")
        void getById_shouldReturn404_whenNotFound() throws Exception {
            mvc.perform(get("/api/drugs/{id}", 999_999))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("GET /api/drugs/{id} -> 200 when entity exists (seeded)")
        @Transactional
        void getById_shouldReturn200_whenExists_seeded() throws Exception {
            DrugFormEntity form = drugFormService.resolve(DrugFormDTO.PILLS);

            DrugEntity entity = DrugEntity.builder()
                    .drugName("Seeded-Drug")
                    .drugForm(form)
                    .expirationDate(DateUtils.buildExpirationDate(OffsetDateTime.now().getYear() + 1, 12))
                    .drugDescription("seeded")
                    .alertSent(false)
                    .build();
            DrugEntity saved = drugRepository.saveAndFlush(entity);

            mvc.perform(get("/api/drugs/{id}", saved.getDrugId()))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        }
    }

    @Nested
    @DisplayName("PDF Export API")
    class PdfExportTests {
        @Test
        @DisplayName("GET /api/drugs/export/pdf -> 400 when size > 100")
        void exportPdf_shouldReject_whenSizeOver100() throws Exception {
            mvc.perform(get("/api/drugs/export/pdf")
                            .param("size", "101"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("GET /api/drugs/export/pdf -> 200 for valid export")
        void exportPdf_shouldReturn200_whenValidExport() throws Exception {
            // given (stub PDF service)
            given(pdfExportService.generatePdf(anyList()))
                    .willReturn("dummy-pdf".getBytes());

            // when + then
            mvc.perform(get("/api/drugs/export/pdf")
                            .param("page", "0")
                            .param("size", "10")
                            .param("sort", "drugName,asc"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith("application/pdf"))
                    .andExpect(header().string("Content-Disposition", containsString("inline")))
                    .andExpect(header().string("Content-Disposition", containsString("drugs_list.pdf")))
                    .andExpect(result -> {
                        byte[] bytes = result.getResponse().getContentAsByteArray();
                        assertThat(bytes).isNotEmpty();
                    });
        }

        @Test
        @DisplayName("GET /api/drugs/export/pdf -> 200 with filters applied")
        void exportPdf_shouldReturn200_withFilters() throws Exception {
            given(pdfExportService.generatePdf(anyList())).willReturn("dummy-pdf".getBytes());

            mvc.perform(get("/api/drugs/export/pdf")
                            .param("name", "asp")
                            .param("form", "PILLS")
                            .param("expired", "false")
                            .param("page", "0")
                            .param("size", "10")
                            .param("sort", "drugName,asc"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith("application/pdf"))
                    .andExpect(header().string("Content-Disposition", containsString("inline")))
                    .andExpect(header().string("Content-Disposition", containsString("drugs_list.pdf")));
        }
    }

    @Nested
    @DisplayName("Statistics API")
    class StatisticsTests {
        @Test
        @DisplayName("GET /api/drugs/statistics -> 200")
        void getStatistics_shouldReturn200() throws Exception {
            mvc.perform(get("/api/drugs/statistics"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
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
                            .param("drugForm", "PILLS")
                            .param("expirationDate", "2023-01-01"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("GET /api/drugs/add -> 400 for invalid form")
        void addDrug_shouldReturn400_whenInvalidForm() throws Exception {
            mvc.perform(get("/api/drugs/add")
                            .param("drugName", "Aspirin")
                            .param("drugForm", "INVALID_FORM")
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
            String json = buildJson("Aspirin", "PILLS",
                    OffsetDateTime.now().plusYears(1).getYear(),
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
            String json = buildJson("Aspirin", "INVALID_FORM",
                    OffsetDateTime.now().plusYears(1).getYear(),
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
        @DisplayName("GET /api/drugs/unknown-endpoint -> 404 Not Found")
        void unknownEndpoint_shouldReturn404() throws Exception {
            mvc.perform(get("/api/drugs/unknown-endpoint/extra"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("GET /api/drugs/unknown-endpoint -> 404 Not Found")
        void unrecognizeEndpoint_shouldReturn400() throws Exception {
            mvc.perform(get("/api/drugs/unknown-endpoint"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("GET /api/drugs/search -> 200 with multiple sort fields")
        void search_shouldReturn200_withMultipleSortFields() throws Exception {
            mvc.perform(get("/api/drugs/search")
                            .param("sort", "drugName,desc")
                            .param("sort", "drugForm.name,asc")
                            .param("size", "5"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("GET /api/drugs/search -> 400 when month < 1")
        void search_shouldReturn400_whenMonthBelowMin() throws Exception {
            mvc.perform(get("/api/drugs/search").param("expirationUntilMonth", "0"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("UpdateDrugPutTests")
    class UpdateDrugPutTests {
        @Test
        @DisplayName("PUT /api/drugs/{id} -> 204 when entity exists (seeded)")
        @Transactional
        void update_shouldReturn204_whenEntityExists_seeded() throws Exception {
            DrugFormEntity form = drugFormRepository.findAll().stream().findFirst().orElseThrow();
            DrugEntity entity = DrugEntity.builder()
                    .drugName("Update-Drug")
                    .drugForm(form)
                    .expirationDate(DateUtils.buildExpirationDate(OffsetDateTime.now().getYear() + 1, 12))
                    .drugDescription("update test")
                    .alertSent(false)
                    .build();
            DrugEntity saved = drugRepository.saveAndFlush(entity);

            String payload = """
                    {
                        "name": "Updated Name",
                        "form": "%s",
                        "expirationYear": %d,
                        "expirationMonth": 12,
                        "description": "Updated description"
                    }
                    """.formatted(form.getName(), OffsetDateTime.now().getYear() + 2);

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
                        "form": "PILLS",
                        "expirationYear": %d,
                        "expirationMonth": 12,
                        "description": "Updated description"
                    }
                    """.formatted(OffsetDateTime.now().getYear() + 2);

            mvc.perform(put("/api/drugs/{id}", 999999)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DeleteDrugTests")
    class DeleteDrugTests {
        @Test
        @DisplayName("DELETE /api/drugs/{id} -> 204 when entity exists (seeded)")
        @Transactional
        void delete_shouldReturn204_whenEntityExists_seeded() throws Exception {
            DrugFormEntity form = drugFormRepository.findAll().stream().findFirst().orElseThrow();
            DrugEntity entity = DrugEntity.builder()
                    .drugName("Delete-Drug")
                    .drugForm(form)
                    .expirationDate(DateUtils.buildExpirationDate(OffsetDateTime.now().getYear() + 1, 12))
                    .drugDescription("delete test")
                    .alertSent(false)
                    .build();
            DrugEntity saved = drugRepository.saveAndFlush(entity);

            mvc.perform(delete("/api/drugs/{id}", saved.getDrugId()))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("DELETE /api/drugs/{id} -> 404 when entity not found")
        void delete_shouldReturn404_whenNotFound() throws Exception {
            mvc.perform(delete("/api/drugs/{id}", 999999))
                    .andExpect(status().isNotFound());
        }
    }
}