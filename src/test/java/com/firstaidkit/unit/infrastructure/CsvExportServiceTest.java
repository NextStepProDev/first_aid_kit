package com.firstaidkit.unit.infrastructure;

import com.firstaidkit.controller.dto.drug.DrugFormDTO;
import com.firstaidkit.controller.dto.drug.DrugResponse;
import com.firstaidkit.infrastructure.csv.CsvExportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CsvExportServiceTest {

    private CsvExportService csvExportService;

    @BeforeEach
    void setUp() {
        csvExportService = new CsvExportService();
    }

    @Nested
    @DisplayName("generateCsv")
    class GenerateCsv {

        @Test
        @DisplayName("should generate CSV with BOM and header for empty list")
        void shouldGenerateHeaderForEmptyList() {
            byte[] result = csvExportService.generateCsv(Collections.emptyList());
            String csv = new String(result, StandardCharsets.UTF_8);

            // Check BOM is present (UTF-8 BOM)
            assertThat(result[0]).isEqualTo((byte) 0xEF);
            assertThat(result[1]).isEqualTo((byte) 0xBB);
            assertThat(result[2]).isEqualTo((byte) 0xBF);

            // Check header
            assertThat(csv).contains("Nazwa;Forma;Data ważności;Opis");
        }

        @Test
        @DisplayName("should generate CSV with drug data")
        void shouldGenerateCsvWithDrugData() {
            OffsetDateTime expirationDate = OffsetDateTime.of(2026, 6, 15, 0, 0, 0, 0, ZoneOffset.UTC);
            DrugResponse drug = DrugResponse.builder()
                    .drugId(1)
                    .drugName("Apap")
                    .drugForm(DrugFormDTO.PILLS)
                    .expirationDate(expirationDate)
                    .drugDescription("Lek przeciwbólowy")
                    .build();

            byte[] result = csvExportService.generateCsv(List.of(drug));
            String csv = new String(result, StandardCharsets.UTF_8);

            assertThat(csv).contains("Nazwa;Forma;Data ważności;Opis");
            assertThat(csv).contains("\"Apap\";PILLS;2026-06-15;\"Lek przeciwbólowy\"");
        }

        @Test
        @DisplayName("should handle multiple drugs")
        void shouldHandleMultipleDrugs() {
            OffsetDateTime date1 = OffsetDateTime.of(2026, 6, 15, 0, 0, 0, 0, ZoneOffset.UTC);
            OffsetDateTime date2 = OffsetDateTime.of(2027, 12, 31, 0, 0, 0, 0, ZoneOffset.UTC);

            DrugResponse drug1 = DrugResponse.builder()
                    .drugId(1)
                    .drugName("Apap")
                    .drugForm(DrugFormDTO.PILLS)
                    .expirationDate(date1)
                    .drugDescription("Lek przeciwbólowy")
                    .build();

            DrugResponse drug2 = DrugResponse.builder()
                    .drugId(2)
                    .drugName("Ibuprom")
                    .drugForm(DrugFormDTO.GEL)
                    .expirationDate(date2)
                    .drugDescription("Żel przeciwbólowy")
                    .build();

            byte[] result = csvExportService.generateCsv(List.of(drug1, drug2));
            String csv = new String(result, StandardCharsets.UTF_8);

            String[] lines = csv.split("\n");
            assertThat(lines).hasSize(3); // header + 2 drugs
            assertThat(lines[1]).contains("Apap");
            assertThat(lines[2]).contains("Ibuprom");
        }

        @Test
        @DisplayName("should escape double quotes in fields")
        void shouldEscapeDoubleQuotes() {
            OffsetDateTime expirationDate = OffsetDateTime.of(2026, 6, 15, 0, 0, 0, 0, ZoneOffset.UTC);
            DrugResponse drug = DrugResponse.builder()
                    .drugId(1)
                    .drugName("Lek \"specjalny\"")
                    .drugForm(DrugFormDTO.PILLS)
                    .expirationDate(expirationDate)
                    .drugDescription("Opis z \"cudzysłowem\"")
                    .build();

            byte[] result = csvExportService.generateCsv(List.of(drug));
            String csv = new String(result, StandardCharsets.UTF_8);

            // Double quotes should be escaped as ""
            assertThat(csv).contains("\"Lek \"\"specjalny\"\"\"");
            assertThat(csv).contains("\"Opis z \"\"cudzysłowem\"\"\"");
        }

        @Test
        @DisplayName("should handle null description")
        void shouldHandleNullDescription() {
            OffsetDateTime expirationDate = OffsetDateTime.of(2026, 6, 15, 0, 0, 0, 0, ZoneOffset.UTC);
            DrugResponse drug = DrugResponse.builder()
                    .drugId(1)
                    .drugName("Apap")
                    .drugForm(DrugFormDTO.PILLS)
                    .expirationDate(expirationDate)
                    .drugDescription(null)
                    .build();

            byte[] result = csvExportService.generateCsv(List.of(drug));
            String csv = new String(result, StandardCharsets.UTF_8);

            assertThat(csv).contains("\"Apap\";PILLS;2026-06-15;\"\"");
        }

        @Test
        @DisplayName("should use semicolon as separator for Polish Excel compatibility")
        void shouldUseSemicolonSeparator() {
            OffsetDateTime expirationDate = OffsetDateTime.of(2026, 6, 15, 0, 0, 0, 0, ZoneOffset.UTC);
            DrugResponse drug = DrugResponse.builder()
                    .drugId(1)
                    .drugName("Apap")
                    .drugForm(DrugFormDTO.PILLS)
                    .expirationDate(expirationDate)
                    .drugDescription("Opis")
                    .build();

            byte[] result = csvExportService.generateCsv(List.of(drug));
            String csv = new String(result, StandardCharsets.UTF_8);

            // Count semicolons in header - should be 3 (between 4 columns)
            String header = csv.split("\n")[0];
            long semicolonCount = header.chars().filter(ch -> ch == ';').count();
            assertThat(semicolonCount).isEqualTo(3);
        }

        @Test
        @DisplayName("should handle description with semicolons (no extra escaping needed since field is quoted)")
        void shouldHandleDescriptionWithSemicolons() {
            OffsetDateTime expirationDate = OffsetDateTime.of(2026, 6, 15, 0, 0, 0, 0, ZoneOffset.UTC);
            DrugResponse drug = DrugResponse.builder()
                    .drugId(1)
                    .drugName("Pimafucort")
                    .drugForm(DrugFormDTO.OINTMENT)
                    .expirationDate(expirationDate)
                    .drugDescription("zapalenie skóry; wyprysk; łuszczyca")
                    .build();

            byte[] result = csvExportService.generateCsv(List.of(drug));
            String csv = new String(result, StandardCharsets.UTF_8);

            // The description with semicolons should be preserved inside quotes
            assertThat(csv).contains("\"zapalenie skóry; wyprysk; łuszczyca\"");
        }
    }
}
