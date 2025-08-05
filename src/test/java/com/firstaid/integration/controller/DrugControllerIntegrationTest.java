package com.firstaid.integration.controller;

import com.firstaid.controller.dto.*;
import com.firstaid.controller.exception.EmailSendingException;
import com.firstaid.controller.exception.ErrorMessage;
import com.firstaid.infrastructure.database.entity.DrugEntity;
import com.firstaid.infrastructure.database.entity.DrugFormEntity;
import com.firstaid.infrastructure.database.repository.DrugFormRepository;
import com.firstaid.infrastructure.database.repository.DrugRepository;
import com.firstaid.infrastructure.email.EmailService;
import com.firstaid.integration.AbstractIntegrationTest;
import com.firstaid.util.DrugRequestDtoBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;

class DrugControllerIntegrationTest extends AbstractIntegrationTest {

    @LocalServerPort
    @SuppressWarnings("unused")
    private int port;

    @Autowired
    @SuppressWarnings("unused")
    private TestRestTemplate restTemplate;

    @MockitoBean
    @SuppressWarnings("unused")
    private EmailService emailService;

    @Autowired
    @SuppressWarnings("unused")
    private DrugRepository drugsRepository;

    @Autowired
    @SuppressWarnings("unused")
    private DrugFormRepository drugsFormRepository;

    private String getUrl(String path) {
        return "http://localhost:" + port + path;
    }

    private void createAndPost6Drugs() {
        DrugRequestDTO drug1 = DrugRequestDtoBuilder.getValidDrugRequestDto();
        DrugRequestDTO drug2 = drug1.toBuilder().name("Altacet").form("GEL").build();
        DrugRequestDTO drug3 = drug1.toBuilder().name("xifaxan").form("PILLS").build();
        DrugRequestDTO drug4 = drug1.toBuilder().name("Naproxen").form("GEL").build();
        DrugRequestDTO drug5 = drug1.toBuilder().name("NOSE").form("DROPS").expirationYear(OffsetDateTime.now()
                .getYear()).expirationMonth(OffsetDateTime.now().getMonthValue()).build();
        DrugRequestDTO drug6 = drug1.toBuilder().build();

        restTemplate.postForEntity(getUrl("/api/drugs"), drug1, DrugDTO.class);
        restTemplate.postForEntity(getUrl("/api/drugs"), drug2, DrugDTO.class);
        restTemplate.postForEntity(getUrl("/api/drugs"), drug3, DrugDTO.class);
        restTemplate.postForEntity(getUrl("/api/drugs"), drug4, DrugDTO.class);
        restTemplate.postForEntity(getUrl("/api/drugs"), drug5, DrugDTO.class);
        restTemplate.postForEntity(getUrl("/api/drugs"), drug6, DrugDTO.class);
    }

    @Nested
    @DisplayName("POST /api/drugs")
    class CreateDrug {

        @Test
        @DisplayName("should create drug when input is valid")
        void shouldCreateDrugWhenInputIsValid() {
            // given
            DrugRequestDTO request = DrugRequestDtoBuilder.getValidDrugRequestDto();

            // when
            var response = restTemplate.postForEntity(getUrl("/api/drugs"), request, DrugDTO.class);

            // then
            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getDrugName()).isEqualTo("Aspirin");
            assertThat(response.getBody().getDrugForm()).isEqualTo(DrugFormDTO.PILLS);
            assertThat(response.getBody().getDrugDescription()).isEqualTo("A common pain reliever");
        }

        @Test
        @DisplayName("should return 400 when input is invalid")
        void shouldReturnBadRequestWhenInputIsInvalid() {
            // given
            DrugRequestDTO base = DrugRequestDtoBuilder.getValidDrugRequestDto();
            DrugRequestDTO invalid = base.toBuilder().form("INVALID_FORM").build();

            // when
            var response = restTemplate.postForEntity(getUrl("/api/drugs"), invalid, String.class);

            // then
            assertThat(response.getStatusCode().is4xxClientError()).isTrue();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody()).contains("Validation failed");
            assertThat(response.getBody()).contains("must be one of");
        }

        @Test
        @DisplayName("should return 400 when input is invalid")
        void shouldReturnBadRequestWhenInputIsBlank() {
            // given
            DrugRequestDTO base = DrugRequestDtoBuilder.getValidDrugRequestDto();
            DrugRequestDTO invalid = base.toBuilder().name("").build();

            // when
            var response = restTemplate.postForEntity(getUrl("/api/drugs"), invalid, String.class);

            // then
            assertThat(response.getStatusCode().is4xxClientError()).isTrue();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody()).contains("Validation failed");
            assertThat(
                    response.getBody().contains("Drug name must not be blank") ||
                            response.getBody().contains("Drug name must be between 2 and 100 characters")
            ).isTrue();
        }
    }

    @Nested
    @DisplayName("GET /api/drugs/{id}")
    class GetDrug {

        @Test
        @DisplayName("should return drug when found")
        void shouldReturnDrugWhenExists() {
            // given
            DrugRequestDTO request = DrugRequestDtoBuilder.getValidDrugRequestDto();

            // when
            var post = restTemplate.postForEntity(getUrl("/api/drugs"), request, DrugDTO.class);
            Integer id = Objects.requireNonNull(post.getBody()).getDrugId();
            var get = restTemplate.getForEntity(getUrl("/api/drugs/" + id), DrugDTO.class);

            // then
            assertThat(get.getStatusCode().is2xxSuccessful()).isTrue();
            assertThat(get.getBody()).isNotNull();
            assertThat(get.getBody().getDrugId()).isEqualTo(id);
            assertThat(get.getBody().getDrugName()).isEqualTo("Aspirin");
            assertThat(get.getBody().getDrugForm()).isEqualTo(DrugFormDTO.PILLS);
            assertThat(get.getBody().getDrugDescription()).isEqualTo("A common pain reliever");
        }

        @Test
        @DisplayName("should return 404 when drug not found")
        void shouldReturnNotFoundWhenDrugDoesNotExist() {
            // when
            var get = restTemplate.getForEntity(getUrl("/api/drugs/2"), String.class);

            // then
            String body = get.getBody();
            assertThat(body).isNotNull();
            assertThat(body).contains("\"status\":404");
            assertThat(body).contains("Drug not found with ID");
        }
    }

    @Nested
    @DisplayName("PUT /api/drugs/{id}")
    class UpdateDrug {

        @Test
        @DisplayName("should update drug when exists")
        void shouldUpdateDrugWhenExists() {
            // given
            DrugRequestDTO request = DrugRequestDtoBuilder.getValidDrugRequestDto();

            // when
            var post = restTemplate.postForEntity(getUrl("/api/drugs"), request, DrugDTO.class);
            Integer id = Objects.requireNonNull(post.getBody()).getDrugId();

            // update
            request.setName("Updated Paracetamol");

            var put = restTemplate.exchange(getUrl("/api/drugs/" + id), HttpMethod.PUT, new HttpEntity<>(request),
                    Void.class);

            // then
            assertThat(put.getStatusCode().is2xxSuccessful()).isTrue();
        }

        @Test
        @DisplayName("should return 404 when trying to update non-existing drug")
        void shouldReturnNotFoundWhenUpdatingNonExistingDrug() {
            // given
            DrugRequestDTO request = DrugRequestDtoBuilder.getValidDrugRequestDto();

            // when
            ResponseEntity<String> response = restTemplate.exchange(
                    getUrl("/api/drugs/9999"),
                    HttpMethod.PUT,
                    new HttpEntity<>(request),
                    String.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).contains("Drug not found with ID");
        }
    }

    @Nested
    @DisplayName("DELETE /api/drugs/{id}")
    class DeleteDrug {

        @Test
        @DisplayName("should delete drug when exists")
        void shouldDeleteDrugWhenExistsAndReturn404WhenFetchingAfter() {
            // given
            DrugRequestDTO request = DrugRequestDtoBuilder.getValidDrugRequestDto();
            var post = restTemplate.postForEntity(getUrl("/api/drugs"), request, DrugDTO.class);
            Integer id = Objects.requireNonNull(post.getBody()).getDrugId();
            assertThat(id).isNotNull();

            // when
            var delete = restTemplate.exchange(getUrl("/api/drugs/" + id),
                    HttpMethod.DELETE, null, Void.class);

            // then
            assertThat(delete.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            var get = restTemplate.getForEntity(getUrl("/api/drugs/" + id), String.class);
            assertThat(get.getStatusCode().is4xxClientError()).isTrue();
            assertThat(get.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(get.getBody()).isNotNull();
            assertThat(get.getBody()).contains("Drug not found with ID");
        }

        @Test
        @DisplayName("should return 404 when trying to delete non-existing drug")
        void shouldReturnNotFoundWhenDeletingNonExistingDrug() {
            // when
            ResponseEntity<String> response = restTemplate.exchange(
                    getUrl("/api/drugs/9999"),
                    HttpMethod.DELETE,
                    null,
                    String.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).contains("Drug not found with ID");
        }
    }

    @Nested
    @DisplayName("GET /api/drugs/statistics")
    class GetDrugStatistics {

        @Test
        @DisplayName("should return statistics")
        void shouldReturnStatisticsWhenRequested() {
            // given
            createAndPost6Drugs();

            // when
            ResponseEntity<DrugStatisticsDTO> response = restTemplate.exchange(
                    getUrl("/api/drugs/statistics"),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {
                    }
            );
            DrugStatisticsDTO statisticsDTO = response.getBody();

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
            assertThat(statisticsDTO).isNotNull();
            assertThat(statisticsDTO.getTotalDrugs()).isEqualTo(6);
            assertThat(statisticsDTO.getDrugsByForm()).isNotNull();
            assertThat(statisticsDTO.getDrugsByForm().size()).isEqualTo(3);
            assertThat(statisticsDTO.getDrugsByForm().get("PILLS")).isEqualTo(3);
            assertThat(statisticsDTO.getDrugsByForm().get("GEL")).isEqualTo(2);
            assertThat(statisticsDTO.getDrugsByForm().get("DROPS")).isEqualTo(1);
            assertThat(statisticsDTO.getDrugsByForm().getOrDefault("OTHER", 0L)).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("GET /api/drugs/export/pdf")
    class ExportPdf {

        @Test
        @DisplayName("should return PDF response")
        void shouldReturnPdfWhenExportRequested() {
            // given
            createAndPost6Drugs();

            // when
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    getUrl("/api/drugs/export/pdf"),
                    HttpMethod.GET,
                    null,
                    byte[].class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getHeaders().getContentType()).isEqualTo(org.springframework.http.MediaType.APPLICATION_PDF);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().length).isGreaterThan(0);
            assertThat(response.getHeaders().getContentDisposition()).isNotNull();
            assertThat(response.getHeaders().getContentDisposition().getFilename()).isEqualTo("drugs_list.pdf");
            assertThat(response.getHeaders().getContentDisposition().getType()).isEqualTo("inline");
            assertThat(response.getBody().length).isGreaterThan(100);
        }
    }

    @Nested
    @DisplayName("GET /api/drugs/expired")
    class GetExpiredDrugs {

        @Test
        @DisplayName("should return list of expired drugs")
        void shouldReturnDrugsWhenExpired() {
            // given
            DrugFormEntity form = drugsFormRepository.findByNameIgnoreCase("PILLS")
                    .orElseThrow(() -> new RuntimeException("Missing form"));

            // dodaj przeterminowany lek
            DrugEntity expiredDrug = new DrugEntity();
            expiredDrug.setDrugName("Expiredin");
            expiredDrug.setDrugDescription("Expired");
            expiredDrug.setExpirationDate(OffsetDateTime.now().minusMonths(2));
            expiredDrug.setDrugForm(form);
            expiredDrug.setAlertSent(false);

            drugsRepository.save(expiredDrug);
            // when
            ResponseEntity<List<DrugDTO>> response = restTemplate.exchange(
                    getUrl("/api/drugs/expired"),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {
                    }
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            List<DrugDTO> expiredDrugs = response.getBody();
            assertThat(expiredDrugs).isNotNull();
            assertThat(expiredDrugs).hasSize(1);
            assertThat(expiredDrugs.getFirst().getDrugName()).isEqualTo("Expiredin");
        }
    }

    @Nested
    @DisplayName("GET /api/drugs/expiration-until")
    class GetDrugsExpiringUntil {

        @Test
        @DisplayName("should return drugs expiring until given date")
        void shouldReturnDrugsWhenExpiringUntilDate() {
            // given
            createAndPost6Drugs();

            // when
            OffsetDateTime untilDate = OffsetDateTime.now().plusYears(1);
            int year = OffsetDateTime.now().plusYears(1).getYear();
            int month = OffsetDateTime.now().getMonthValue();
            ResponseEntity<List<DrugDTO>> response = restTemplate.exchange(
                    getUrl("/api/drugs/expiration-until?year=" + year + "&month=" + month),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {
                    }
            );
            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            List<DrugDTO> drugs = response.getBody();
            assertThat(drugs).isNotNull();
            assertThat(drugs).hasSize(6);
            for (DrugDTO drug : drugs) {
                assertThat(drug.getExpirationDate()).isBeforeOrEqualTo(untilDate);
            }
        }
    }

    @Nested
    @DisplayName("GET /api/drugs/by-description")
    class GetDrugsByDescription {

        @Test
        @DisplayName("should return drugs containing given description")
        void shouldReturnDrugsWhenDescriptionMatches() {
            // given
            createAndPost6Drugs();

            // when
            ResponseEntity<List<DrugDTO>> response = restTemplate.exchange(
                    getUrl("/api/drugs/by-description?description=common"),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {
                    }
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            List<DrugDTO> drugs = response.getBody();
            assertThat(drugs).isNotNull();
            assertThat(drugs).hasSize(6);
            assertThat(drugs.getFirst().getDrugName()).isEqualTo("Aspirin");
        }
    }

    @Nested
    @DisplayName("GET /api/drugs/by-form")
    class GetDrugsByForm {

        @Test
        @DisplayName("should return drugs with given form")
        void shouldReturnDrugsWhenFormMatches() {
            // given
            createAndPost6Drugs();

            // when
            ResponseEntity<List<DrugDTO>> response = restTemplate.exchange(
                    getUrl("/api/drugs/by-form?form=GEL"),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {
                    }
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            List<DrugDTO> drugs = response.getBody();
            assertThat(drugs).isNotNull();
            assertThat(drugs).hasSize(2);
            assertThat(drugs.getFirst().getDrugForm()).isEqualTo(DrugFormDTO.GEL);
            assertThat(drugs.getFirst().getDrugName()).isEqualTo("Altacet");
            assertThat(drugs.get(1).getDrugName()).isEqualTo("Naproxen");
        }

        @Test
        void shouldReturn400_whenInvalidDrugFormGiven() {
            ResponseEntity<String> response = restTemplate.exchange(
                    getUrl("/api/drugs/by-form?form=INVALID"),
                    HttpMethod.GET,
                    null,
                    String.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).contains("Invalid drug form: INVALID");
        }
    }

    @Nested
    @DisplayName("GET /api/drugs/by-name")
    class GetDrugsByName {

        @Test
        @DisplayName("should return drugs with given name")
        void shouldReturnDrugsWhenNameMatches() {
            // given
            createAndPost6Drugs();

            // when
            ResponseEntity<List<DrugDTO>> response = restTemplate.exchange(
                    getUrl("/api/drugs/by-name?name=aspirin"),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {
                    }
            );
            List<DrugDTO> drugs = response.getBody();

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(drugs).isNotNull();
            assertThat(drugs.getFirst().getDrugName()).isEqualTo("Aspirin");
        }

        @Test
        @DisplayName("should return 400 when name only digits")
        void shouldReturn400_whenNameIsOnlyDigits() {
            // given
            String request = "123456";

            // when
            var response = restTemplate.getForEntity(
                    "/api/drugs//by-name?name={name}", String.class, request);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).contains("Name must contain at least one non-digit character");
        }
    }

    @Nested
    @DisplayName("GET /api/drugs/sorted")
    class GetSortedDrugs {

        @Test
        @DisplayName("should return sorted drugs")
        void shouldReturnDrugsWhenSorted() {
            // given
            createAndPost6Drugs();

            // when
            ResponseEntity<List<DrugDTO>> response = restTemplate.exchange(
                    getUrl("/api/drugs/sorted?sortBy=name&direction=ASC"),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {
                    }
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            List<DrugDTO> drugs = response.getBody();
            assertThat(drugs).isNotNull();
            assertThat(drugs).hasSize(6);
            assertThat(drugs.getFirst().getDrugName()).isEqualTo("Altacet");
        }
    }

    @Nested
    @DisplayName("GET /api/drugs/paged")
    class GetPagedDrugs {

        @Test
        @DisplayName("should return paged list of drugs")
        void shouldReturnDrugsWhenPaged() {
            // given
            createAndPost6Drugs();

            // when
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    getUrl("/api/drugs/paged?page=0&size=3"),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {
                    }
            );

            // then
            Assertions.assertNotNull(response.getBody());
            List<?> content = (List<?>) response.getBody().get("content");
            assertThat(content).hasSize(3);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().get("totalElements")).isEqualTo(6);
            assertThat(response.getBody().get("totalPages")).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("GET /api/drugs/forms")
    class GetDrugForms {

        @Test
        @DisplayName("should return all available drug forms")
        void shouldReturnAvailableDrugForms() {
            // when
            ResponseEntity<List<FormOption>> response = restTemplate.exchange(
                    getUrl("/api/drugs/forms"),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {
                    }
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            List<FormOption> forms = response.getBody();
            assertThat(forms).isNotNull();
            assertThat(forms).extracting(FormOption::value)
                    .contains("pills", "gel", "drops", "syrup", "other");
            assertThat(forms.size()).isEqualTo(DrugFormDTO.values().length);
        }
    }

    @Nested
    @DisplayName("GET /api/drugs/forms/dictionary")
    class GetDrugFormsDictionary {

        @Test
        @DisplayName("should return form labels dictionary")
        void shouldReturnFormLabelsDictionary() {
            // when
            ResponseEntity<Map<String, String>> response = restTemplate.exchange(
                    getUrl("/api/drugs/forms/dictionary"),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {
                    }
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            Map<String, String> dictionary = response.getBody();
            assertThat(dictionary).isNotNull();
            assertThat(dictionary).containsEntry("PILLS", "Tabletki");
            assertThat(dictionary).containsEntry("GEL", "Żel");
            assertThat(dictionary).containsEntry("DROPS", "Krople");
            assertThat(dictionary).containsEntry("SYRUP", "Syrop");
            assertThat(dictionary).containsEntry("OTHER", "Inne");
            assertThat(dictionary.size()).isEqualTo(Arrays.stream(DrugFormDTO.values()).toList().size());
        }
    }

    @Nested
    @DisplayName("GET /api/drugs/simple")
    class GetSimpleDrugs {

        @Test
        @DisplayName("should return simplified list of drugs")
        void shouldReturnSimpleDrugsList() {
            // given
            createAndPost6Drugs();

            // when
            ResponseEntity<List<DrugSimpleDTO>> response = restTemplate.exchange(
                    getUrl("/api/drugs/simple"),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {
                    }
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            List<DrugSimpleDTO> simpleDrugs = response.getBody();
            assertThat(simpleDrugs).isNotNull();
            assertThat(simpleDrugs).hasSize(6);
            assertThat(simpleDrugs.getFirst().getDrugName()).isEqualTo("Aspirin");
            for (DrugSimpleDTO drug : simpleDrugs) {
                assertThat(drug.getDrugId()).isNotNull();
                assertThat(drug.getDrugName()).isNotBlank();
                assertThat(drug.getDrugForm()).isNotNull();
                assertThat(drug.getExpirationDate()).isNotNull();
            }
        }
    }

    @Nested
    @DisplayName("POST /api/email/alerts")
    class SendExpiryAlert {

        @Test
        @DisplayName("should return 500 INTERNAL_SERVER_ERROR when email sending fails")
        void shouldReturnInternalServerErrorWhenEmailSendingFails() {
            // given
            OffsetDateTime now = OffsetDateTime.now();
            DrugRequestDTO request = DrugRequestDtoBuilder.getValidDrugRequestDto()
                    .toBuilder()
                    .expirationYear(now.getYear())
                    .expirationMonth(now.getMonthValue())
                    .build();
            restTemplate.postForEntity(getUrl("/api/drugs"), request, DrugDTO.class);

            // and email will fail
            doThrow(new EmailSendingException("Simulated failure", new RuntimeException("SMTP timeout")))
                    .when(emailService)
                    .sendEmail(anyString(), anyString(), anyString());

            // when
            ResponseEntity<String> response = restTemplate.postForEntity(getUrl("/api/email/alerts"), null, String.class);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).contains("Failed to send expiry alert email");
        }
    }

    @Nested
    @DisplayName("GET /api/drugs/sorted")
    class GetDrugsSortedTests {

        @Test
        @DisplayName("should return drugs sorted by name in ascending order")
        void shouldReturnDrugsSortedByNameAsc() {
            // given
            DrugRequestDTO drug1 = DrugRequestDtoBuilder.getValidDrugRequestDto()
                    .toBuilder().name("Paracetamol").build();
            DrugRequestDTO drug2 = drug1.toBuilder().name("Ibuprofen").build();
            DrugRequestDTO drug3 = drug1.toBuilder().name("Aspirin").build();

            restTemplate.postForEntity(getUrl("/api/drugs"), drug1, DrugDTO.class);
            restTemplate.postForEntity(getUrl("/api/drugs"), drug2, DrugDTO.class);
            restTemplate.postForEntity(getUrl("/api/drugs"), drug3, DrugDTO.class);

            // when
            ResponseEntity<List<DrugDTO>> response = restTemplate.exchange(
                    getUrl("/api/drugs/sorted?sortBy=name&direction=asc"),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {
                    }
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            List<DrugDTO> drugs = response.getBody();
            assertThat(drugs).isNotNull();
            assertThat(drugs).extracting(DrugDTO::getDrugName)
                    .containsExactly("Aspirin", "Ibuprofen", "Paracetamol");
        }

        @Test
        @DisplayName("should return drugs sorted by name in descending order")
        void shouldReturnDrugsSortedByNameDesc() {
            // given
            DrugRequestDTO drug1 = DrugRequestDtoBuilder.getValidDrugRequestDto()
                    .toBuilder().name("Paracetamol").build();
            DrugRequestDTO drug2 = drug1.toBuilder().name("Ibuprofen").build();
            DrugRequestDTO drug3 = drug1.toBuilder().name("Aspirin").build();

            restTemplate.postForEntity(getUrl("/api/drugs"), drug1, DrugDTO.class);
            restTemplate.postForEntity(getUrl("/api/drugs"), drug2, DrugDTO.class);
            restTemplate.postForEntity(getUrl("/api/drugs"), drug3, DrugDTO.class);

            // when
            ResponseEntity<List<DrugDTO>> response = restTemplate.exchange(
                    getUrl("/api/drugs/sorted?sortBy=name&direction=desc"),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {
                    }
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            List<DrugDTO> drugs = response.getBody();
            assertThat(drugs).isNotNull();
            assertThat(drugs).extracting(DrugDTO::getDrugName)
                    .containsExactly("Paracetamol", "Ibuprofen", "Aspirin");
        }

        @Test
        @DisplayName("should return 400 when direction param is missing")
        void shouldReturn400WhenDirectionParamIsMissing() {
            // when
            ResponseEntity<ErrorMessage> response = restTemplate.exchange(
                    getUrl("/api/drugs/sorted?sortBy=name"),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {
                    }
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            ErrorMessage error = response.getBody();
            assertThat(error).isNotNull();
            assertThat(error.message()).contains("direction"); // lub dokładny komunikat
        }
    }
}

