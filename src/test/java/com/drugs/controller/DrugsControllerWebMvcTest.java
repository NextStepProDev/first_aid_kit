package com.drugs.controller;

import com.drugs.infrastructure.business.DrugsFormService;
import com.drugs.infrastructure.business.DrugsService;
import com.drugs.infrastructure.database.mapper.DrugsMapper;
import com.drugs.infrastructure.database.repository.DrugsRepository;
import com.drugs.infrastructure.mail.EmailService;
import com.drugs.infrastructure.pdf.PdfExportService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.stream.Stream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DrugsController.class)
@Import(DrugsControllerWebMvcTest.NoSecurityConfig.class) // to jest konieczne, ponieważ security blokuje test
public class DrugsControllerWebMvcTest {

    @Autowired
    @SuppressWarnings("unused")
    private MockMvc mockMvc;

    @MockitoBean
    @SuppressWarnings("unused")
    private DrugsRepository drugsRepository;

    @MockitoBean
    @SuppressWarnings("unused")
    private DrugsFormService drugsFormService;

    @MockitoBean
    @SuppressWarnings("unused")
    private DrugsMapper drugsMapper;

    @MockitoBean
    @SuppressWarnings("unused")
    private EmailService emailService;

    @MockitoBean
    @SuppressWarnings("unused")
    private DrugsService drugsService;

    @MockitoBean
    @SuppressWarnings("unused")
    private PdfExportService pdfExportService;

    static Stream<org.junit.jupiter.params.provider.Arguments> formProvider() {
        return Stream.of(
                org.junit.jupiter.params.provider.Arguments.of(true, "PILLS"),
                org.junit.jupiter.params.provider.Arguments.of(true, "SYRUP"),
                org.junit.jupiter.params.provider.Arguments.of(false, "INVALID_FORM"),
                org.junit.jupiter.params.provider.Arguments.of(false, "")
        );
    }

    @ParameterizedTest
    @MethodSource("formProvider")
    void thatFormValidationWorksCorrectly(boolean isValid, String form) throws Exception {
        // language=json
        String json = """
                {
                  "name": "Ibuprofen",
                  "form": "%s",
                  "expirationYear": 2025,
                  "expirationMonth": 12,
                  "description": "lek przeciwbólowy"
                }
                """.formatted(form);

        mockMvc.perform(
                        post("/api/drugs")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(isValid ? status().isCreated() : status().isBadRequest());
    }

    @TestConfiguration
    static class NoSecurityConfig {
        @Bean
        @SuppressWarnings("unused")
        public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .securityMatcher("/**") // jeśli chcesz mieć ogólnie
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .csrf(AbstractHttpConfigurer::disable)
                    .build();
        }
    }
}