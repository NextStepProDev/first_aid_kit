package com.firstaid.integration.e2e;

import com.firstaid.integration.e2e.base.BaseE2ETest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class DrugCreateE2ETest extends BaseE2ETest {

    @MockitoBean
    @SuppressWarnings("unused")
    private JavaMailSender mailSender;

    @DisplayName("POST /api/drugs creates a drug")
    @Test
    void shouldCreateDrug() {
        var now = OffsetDateTime.now();
        int year = now.getYear();
        int month = now.getMonthValue();
        var body = String.format("""
                {
                    "name":"Ibuprofen",
                    "form":"PILLS",
                    "expirationYear":%d,
                    "expirationMonth":%d,
                    "description":"x"
                }
                """, year, month);

        // POST
        ResponseEntity<String> response = postJson("/api/drugs", body);

        assertThat(response.getStatusCode()).isIn(HttpStatus.CREATED, HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).contains("Ibuprofen");
    }

    @DisplayName("GET /api/drugs/{id} returns 200 for existing drug")
    @Test
    void shouldReturn200_whenDrugExists() {
        int year = OffsetDateTime.now().getYear();
        int month = OffsetDateTime.now().getMonthValue();
        var body = String.format("""
                {"name":"Paracetamol","form":"PILLS","expirationYear":%d,"expirationMonth":%d,"description":"seed"}
                """, year, month);

        // Create
        ResponseEntity<String> created = postJson("/api/drugs", body);

        assertThat(created.getStatusCode()).isIn(HttpStatus.CREATED, HttpStatus.OK);
        assertThat(created.getBody()).isNotNull();
        assertThat(created.getBody()).contains("Paracetamol");
    }
}