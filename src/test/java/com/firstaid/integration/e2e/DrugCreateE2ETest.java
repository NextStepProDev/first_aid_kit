package com.firstaid.integration.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.firstaid.integration.e2e.base.BaseE2ETest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.OffsetDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

class DrugCreateE2ETest extends BaseE2ETest {

    @MockitoBean
    @SuppressWarnings("unused")
    private JavaMailSender mailSender;

    @DisplayName("POST /api/drugs creates a drug (201 or 200)")
    @Test
    void shouldCreateDrug() {
        // given
        var now = java.time.OffsetDateTime.now();
        int year = now.getYear();
        int month = now.getMonthValue();
        var body = String.format(
                """
                        {
                            "name":"Ibuprofen",
                            "form":"PILLS","expirationYear":%d,
                            "expirationMonth":%d,"description":"x"
                            }
                """,
                year, month
        );
        // when
        ResponseEntity<String> res = postJson("/api/drugs", body, String.class);

        // then
        assertThat(res.getStatusCode()).isIn(HttpStatus.CREATED, HttpStatus.OK);

        if (res.getStatusCode() == HttpStatus.CREATED) {
            var location = res.getHeaders().getLocation();
            if (location != null) {
                ResponseEntity<String> get = getJson(location.getPath(), String.class);
                assertThat(get.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(get.getBody()).isNotNull();
                assertThat(get.getBody()).contains("Ibuprofen");
            } else {
                ResponseEntity<String> search = getJson("/api/drugs/search?name=Ibuprofen&page=0&size=1",
                        String.class);
                assertThat(search.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(search.getBody()).isNotNull();
                assertThat(search.getBody()).contains("Ibuprofen");
            }
        } else if (res.getStatusCode() == HttpStatus.OK && res.getBody() != null && !res.getBody().isBlank()) {
            assertThat(res.getBody()).contains("Ibuprofen");
        } else {
            ResponseEntity<String> search = getJson("/api/drugs/search?name=Ibuprofen&page=0&size=1", String.class);
            assertThat(search.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(search.getBody()).isNotNull();
            assertThat(search.getBody()).contains("Ibuprofen");
        }
    }

    @DisplayName("GET /api/drugs/{id} returns 200 for existing drug")
    @Test
    void shouldReturn200_whenDrugExists() {
        // given
        int year = OffsetDateTime.now().getYear();
        int month = OffsetDateTime.now().getMonthValue();
        var body = String.format(
                "{\"name\":\"Paracetamol\",\"form\":\"PILLS\",\"expirationYear\":%d,\"expirationMonth\":%d,\"description\":\"seed\"}",
                year, month);

        // when
        ResponseEntity<String> created = postJson("/api/drugs", body, String.class);
        // then
        assertThat(created.getStatusCode()).isIn(HttpStatus.CREATED, HttpStatus.OK);

        String path;
        if (created.getStatusCode() == HttpStatus.CREATED && created.getHeaders().getLocation() != null) {
            path = created.getHeaders().getLocation().getPath();
        } else {
            ResponseEntity<String> search = getJson("/api/drugs/search?name=Paracetamol&page=0&size=1", String.class);
            assertThat(search.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(search.getBody()).isNotNull();
            assertThat(search.getBody()).contains("Paracetamol");
            return;
        }

        // when
        ResponseEntity<String> get = getJson(path, String.class);
        // then
        assertThat(get.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(get.getBody()).isNotNull();
        assertThat(get.getBody()).contains("Paracetamol");
    }

    @DisplayName("GET /api/drugs/{id} returns 404 for missing drug")
    @Test
    void shouldReturn404_whenNotFound() {
        // when
        ResponseEntity<String> get = getJson("/api/drugs/999999", String.class);
        // then
        assertThat(get.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @DisplayName("SEARCH filters by name+form, with paging & sorting")
    @Test
    void shouldSearchByFormAndName_withPagingAndSorting() {
        int year = java.time.OffsetDateTime.now().getYear();
        int month = java.time.OffsetDateTime.now().getMonthValue();

        // given
        postJson("/api/drugs", String.format("""
                {
                    "name":"Ibu Max",
                    "form":"PILLS",
                    "expirationYear":%d,
                    "expirationMonth":%d,
                    "description":"seed"
                    }
                """, year, month), String.class);
        postJson("/api/drugs", String.format("""
                {
                    "name":"Ibum Syrup",
                    "form":"SYRUP","expirationYear":%d,
                    "expirationMonth":%d,
                    "description":"seed"
                }
                """, year, month), String.class);

        // when
        ResponseEntity<String> res = getJson("/api/drugs/search?name=ibu&form=PILLS&expired=false&page=0&size=10&sort=drugName,asc", String.class);
        // then
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody()).contains("Ibu Max");
        assertThat(res.getBody()).doesNotContain("Ibum Syrup");
    }

    @DisplayName("SEARCH defaults year when only month provided")
    @Test
    void shouldDefaultYearWhenOnlyMonthProvided() {
        // when
        ResponseEntity<String> res = getJson("/api/drugs/search?expirationUntilMonth=8&page=0&size=1",
                String.class);
        // then
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @DisplayName("SEARCH returns 400 on invalid sort field")
    @Test
    void shouldReturn400_onInvalidSortField() {
        // when
        ResponseEntity<String> res = getJson("/api/drugs/search?name=x&sort=unknown,asc",
                String.class);
        // then
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @DisplayName("PUT updates drug; subsequent GET reflects changes")
    @Test
    void shouldUpdate_thenGetReflectsChanges() throws Exception {
        int year = OffsetDateTime.now().getYear();
        int month = OffsetDateTime.now().getMonthValue();

        // given
        String createJson = String.format("""
                {
                    "name":"Aspirin",
                    "form":"PILLS",
                    "expirationYear":%d,
                    "expirationMonth":%d,
                    "description":"seed"
                }
                """, year, month);
        ResponseEntity<String> created = postJson("/api/drugs", createJson, String.class);
        assertThat(created.getStatusCode()).isIn(HttpStatus.CREATED, HttpStatus.OK);

        // resolve path: prefer Location, otherwise grab first drugId from /search
        String path;
        if (created.getHeaders().getLocation() != null) {
            path = created.getHeaders().getLocation().getPath();
        } else {
            ResponseEntity<String> search = getJson("/api/drugs/search?name=Aspirin&page=0&size=1", String.class);
            assertThat(search.getStatusCode()).isEqualTo(HttpStatus.OK);
            String body = search.getBody();
            assertThat(body).isNotNull();
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(body);
            JsonNode idNode = root.path("content").path(0).path("drugId");
            assertThat(idNode.isInt()).as("Expected content[0].drugId in search response").isTrue();
            path = "/api/drugs/" + idNode.asInt();
        }

        // when
        var updateJson = String.format("{\"name\":\"Aspirin Forte\",\"form\":\"PILLS\",\"expirationYear\":%d,\"expirationMonth\":%d,\"description\":\"updated\"}", year, month);
        ResponseEntity<Void> put = putJson(path, updateJson, Void.class);
        // then
        assertThat(put.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // when
        ResponseEntity<String> get = getJson(path, String.class);
        // then
        assertThat(get.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(get.getBody()).isNotNull();
        assertThat(get.getBody()).contains("Aspirin Forte");
    }

    @DisplayName("DELETE removes drug; subsequent GET returns 404")
    @Test
    void shouldDelete_then404OnGet() {
        int year = java.time.OffsetDateTime.now().getYear();
        int month = java.time.OffsetDateTime.now().getMonthValue();

        // given
        var createJson = String.format("""
                {
                    "name":"TmpDel",
                    "form":"PILLS",
                    "expirationYear":%d,
                    "expirationMonth":%d,
                    "description":"seed"
                }
                """, year, month);
        ResponseEntity<String> created = postJson("/api/drugs", createJson, String.class);
        assertThat(created.getStatusCode()).isIn(HttpStatus.CREATED, HttpStatus.OK);

        String path = (created.getHeaders().getLocation() != null)
                ? created.getHeaders().getLocation().getPath()
                : null;

        if (path == null) {
            ResponseEntity<String> search = getJson("/api/drugs/search?name=TmpDel&page=0&size=1", String.class);
            assertThat(search.getStatusCode()).isEqualTo(HttpStatus.OK);
            return;
        }

        // when
        ResponseEntity<Void> del = delete(path);
        // then
        assertThat(del.getStatusCode()).isIn(HttpStatus.NO_CONTENT, HttpStatus.OK);

        // when
        ResponseEntity<String> get = getJson(path, String.class);
        // then
        assertThat(get.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @DisplayName("GET /api/drugs/statistics returns expected keys")
    @Test
    void shouldReturnStatistics_withExpectedKeys() {
        // when
        ResponseEntity<String> res = getJson("/api/drugs/statistics", String.class);
        // then
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody())
                .contains("totalDrugs")
                .contains("expiredDrugs")
                .contains("activeDrugs")
                .contains("alertSentCount")
                .contains("drugsByForm");
    }

    @DisplayName("Export PDF with filters returns 200 and PDF")
    @Test
    void shouldExportPdf_withFilters() {
        // when
        ResponseEntity<byte[]> pdf = getBytes("/api/drugs/export/pdf?form=PILLS&size=20&expired=false");
        // then
        assertThat(pdf.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(pdf.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(pdf.getBody()).isNotEmpty();
    }

    @DisplayName("Export PDF returns 400 when size exceeds limit")
    @Test
    void shouldReturn400_whenSizeTooLarge() {
        // when
        ResponseEntity<byte[]> pdf = getBytes("/api/drugs/export/pdf?size=9999");
        // then
        assertThat(pdf.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
    @DisplayName("POST /api/email/alerts triggers default expiry alert")
    @Test
    void shouldTriggerDefaultAlerts_andReturn200() {
        // when
        ResponseEntity<String> res = postJson("/api/email/alert", null, String.class);

        // then
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody()).contains("Expiry alert emails have been sent");
    }

    @DisplayName("POST /api/email/alerts sends emails when recipient configured and expiring drugs exist")
    @Test
    void shouldSendEmails_whenRecipientConfiguredAndExpiring() {
        // ensure EmailService 'from' is present at runtime
        System.setProperty("spring.mail.username", "noreply@test.local");

        // given: seed a drug expiring no later than next month (so it will be picked up)
        OffsetDateTime now = OffsetDateTime.now();
        String createJson = String.format("""
                {
                  "name": "MailTest",
                  "form": "PILLS",
                  "expirationYear": %d,
                  "expirationMonth": %d,
                  "description": "about to expire"
                }
                """, now.getYear(), now.getMonthValue());
        ResponseEntity<String> created = postJson("/api/drugs", createJson, String.class);
        assertThat(created.getStatusCode()).isIn(HttpStatus.CREATED, HttpStatus.OK);

        // when
        ResponseEntity<String> res = postJson("/api/email/alert", null, String.class);

        // then
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody()).contains("Expiry alert emails have been sent");
        // and: verify that mail was attempted (recipient must be configured in test profile via app.alert.recipientEmail)
        verify(mailSender, atLeastOnce()).send(any(SimpleMailMessage.class));
    }
}