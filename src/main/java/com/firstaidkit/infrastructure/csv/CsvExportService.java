package com.firstaidkit.infrastructure.csv;

import com.firstaidkit.controller.dto.drug.DrugResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Service
public class CsvExportService {

    private static final String SEPARATOR = ";";
    private static final String[] HEADERS = {"Nazwa", "Forma", "Data ważności", "Opis"};

    public byte[] generateCsv(List<DrugResponse> drugs) {
        log.info("Starting CSV generation for {} drugs", drugs.size());

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
            // BOM for Excel UTF-8 compatibility
            out.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});

            // Header row
            writer.println(String.join(SEPARATOR, HEADERS));

            // Data rows
            for (DrugResponse drug : drugs) {
                String row = String.join(SEPARATOR,
                        quote(drug.getDrugName()),
                        drug.getDrugForm().name(),
                        drug.getExpirationDate().toLocalDate().toString(),
                        quote(drug.getDrugDescription() != null ? drug.getDrugDescription() : "")
                );
                writer.println(row);
            }

            writer.flush();
            log.info("CSV generation completed successfully");

        } catch (Exception e) {
            log.error("Failed to generate CSV", e);
            throw new RuntimeException("Failed to generate CSV", e);
        }

        return out.toByteArray();
    }

    /**
     * Quotes a field for CSV - escapes double quotes and wraps in quotes
     */
    private String quote(String field) {
        if (field == null) {
            return "\"\"";
        }
        String escaped = field.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}
