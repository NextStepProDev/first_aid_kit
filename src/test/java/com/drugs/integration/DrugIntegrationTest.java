package com.drugs.integration;

import com.jayway.jsonpath.JsonPath;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.OffsetDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
public class DrugIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    @SuppressWarnings("unused")
    private MockMvc mockMvc;

    String url = "/api/drugs/";

    @Test
    @DisplayName("Should insert new drug into database successfully")
    void shouldAddNewDrugSuccessfully() throws Exception {
        int currentYear = OffsetDateTime.now().getYear();

        String requestJson = """
                {
                    "name": "Ibuprofen",
                    "form": "GEL",
                    "description": "Painkiller",
                    "expirationYear": %d,
                    "expirationMonth": 12
                }
                """.formatted(currentYear);

        // 1. Create a drug
        mockMvc.perform(post("/api/drugs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Should return list of drugs including newly added drug")
    void shouldReturnDrugsListWithNewDrug() throws Exception {
        int currentYear = OffsetDateTime.now().getYear();

        String requestJson = """
                {
                    "name": "Paracetamol",
                    "form": "PILLS",
                    "description": "Fever reducer2",
                    "expirationYear": %d,
                    "expirationMonth": 10
                }
                """.formatted(currentYear);

        // 1. Create a drug
        mockMvc.perform(post("/api/drugs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.drugName").value("Paracetamol"));

        // 2. Verify result
        mockMvc.perform(get("/api/drugs")).andDo(print());
        mockMvc.perform(get("/api/drugs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].drugName").value(org.hamcrest.Matchers.hasItem("Paracetamol")));
    }

    @Test
    @DisplayName("Should not return list of drugs including newly added drug")
    void shouldNotReturnDrugsListWithNewDrug() throws Exception {

        String requestJson = """
                {
                    "name": "Semaglutide",
                    "form": "FORM_NOT_FOUND",
                    "description": "Fever reducer",
                    "expirationYear": 2025,
                    "expirationMonth": 10
                }
                """;

        // 1. Try to add drug with invalid form
        mockMvc.perform(post("/api/drugs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // 2. Verify that no drugs are present
        mockMvc.perform(get("/api/drugs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("Should update existing drug and verify updated values")
    void shouldUpdateExistingDrugAndVerifyUpdated() throws Exception {

        String requestJson = """
                {
                    "name": "Paracetamol",
                    "form": "PILLS",
                    "description": "Fever reducer",
                    "expirationYear": %s,
                    "expirationMonth": 10
                }
                """.formatted(OffsetDateTime.now().getYear() + 1);

        String requestJsonUpdated = """
                {
                    "name": "Paracetamol",
                    "form": "GEL",
                    "description": "Fever reducer",
                    "expirationYear": %s,
                    "expirationMonth": 10
                }
                """.formatted(OffsetDateTime.now().getYear() + 1);

        // 1. Create a drug
        MvcResult result = mockMvc.perform(post("/api/drugs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andReturn();

        // 2. Extract the drug ID
        String responseBody = result.getResponse().getContentAsString();
        int drugId = JsonPath.read(responseBody, "$.drugId");

        // 3. Update the drug
        mockMvc.perform(put(url + drugId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJsonUpdated))
                .andExpect(status().isNoContent());

        // 4. Verify result
        mockMvc.perform(get(url + drugId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.drugForm").value("GEL"));
    }

    @Test
    @DisplayName("Should add, retrieve and delete a drug and verify it's removed")
    void shouldAddRetrieveAndDeleteDrugSuccessfully() throws Exception {
        int currentYear = OffsetDateTime.now().getYear();

        String requestJson = """
                {
                    "name": "Paracetamol",
                    "form": "PILLS",
                    "description": "Fever reducer",
                    "expirationYear": %d,
                    "expirationMonth": 10
                }
                """.formatted(currentYear);

        // 1. Create a drug
        MvcResult result = mockMvc.perform(post("/api/drugs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.drugName").value("Paracetamol"))
                .andReturn();


        // 2. Extract the drug ID
        String responseBody = result.getResponse().getContentAsString();
        int drugId = JsonPath.read(responseBody, "$.drugId");

        // 3. Verify the drug exists

        mockMvc.perform(get(url + drugId))
                .andExpect(status().isOk());

        // 3. Delete the drug
        mockMvc.perform(delete(url + drugId))
                .andExpect(status().isNoContent());

        // 4. Verify result
        mockMvc.perform(get(url + drugId))
                .andExpect(status().isNotFound());

    }

    @Test
    @DisplayName("Should filter drugs by form and name (combined filters)")
    void shouldGetWithFiltersSuccessfully() throws Exception {

        // 1. Create a drug
        int currentYear = OffsetDateTime.now().getYear() + 1;
        String requestJson1 = """
                {
                    "name": "Paracetamol",
                    "form": "PILLS",
                    "description": "Painkiller",
                    "expirationYear": %d,
                    "expirationMonth": 10
                }
                """.formatted(currentYear);
        String requestJson2 = """
                {
                    "name": "Ibuprofen",
                    "form": "PILLS",
                    "description": "Painkiller",
                    "expirationYear": %d,
                    "expirationMonth": 4
                }
                """.formatted(currentYear);

        String requestJson3 = """
                {
                    "name": "Altacet",
                    "form": "GEL",
                    "description": "For bruises",
                    "expirationYear": %d,
                    "expirationMonth": 4
                }
                """.formatted(currentYear);

        mockMvc.perform(post("/api/drugs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson1))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/drugs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson2))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/drugs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson3))
                .andExpect(status().isCreated());

        // 1. Get drugs by form
        mockMvc.perform(get("/api/drugs/by-form")
                        .param("form", "PILLS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].drugForm").value(Matchers.hasItem("PILLS")))
                .andExpect(jsonPath("$.length()").value(2));

        // 2. Get drugs by name
        mockMvc.perform(get("/api/drugs/by-name")
                        .param("name", "Altacet"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].drugName").value(Matchers.hasItem("Altacet")))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("Should add assertions for full JSON response content")
    void shouldReturnExpectedJsonStructure() throws Exception {

        int currentYear = OffsetDateTime.now().getYear() + 1;
        String requestJson = """
                {
                    "name": "Paracetamol",
                    "form": "PILLS",
                    "description": "Painkiller",
                    "expirationYear": %d,
                    "expirationMonth": 10
                }
                """.formatted(currentYear);

        mockMvc.perform(post("/api/drugs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/drugs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].drugName").value("Paracetamol"))
                .andExpect(jsonPath("$[0].drugForm").value("PILLS"))
                .andExpect(jsonPath("$[0].drugDescription").value("Painkiller"))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("Should return 404 when drug is not found by ID")
    void shouldReturn404WhenDrugNotFoundById() throws Exception {
        int nonExistingId = 9999;

        mockMvc.perform(get(url + nonExistingId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Drug not found with ID: 9999"));
    }

    @Test
    @DisplayName("Should return drugs by name")
    void shouldReturnDrugsByName() throws Exception {
        int currentYear = OffsetDateTime.now().getYear() + 1;
        String requestJson = """
                {
                    "name": "Altacet",
                    "form": "PILLS",
                    "description": "Painkiller",
                    "expirationYear": %d,
                    "expirationMonth": 10
                }
                """.formatted(currentYear);

        mockMvc.perform(post("/api/drugs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/drugs/by-name")
                        .param("name", "Altacet"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].drugName").value(Matchers.hasItem("Altacet")))
                .andExpect(jsonPath("$.length()").value(1));
    }
}