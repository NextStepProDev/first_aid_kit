package com.firstaid.infrastructure.pdf;

import com.firstaid.controller.dto.DrugDTO;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfExportService {

    private static PdfPTable getPdfPTable(List<DrugDTO> drugs) {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.addCell("LP");
        table.addCell("Name");
        table.addCell("Form");
        table.addCell("Expiration");

        int index = 1;
        for (DrugDTO drug : drugs) {
            table.addCell(String.valueOf(index++));
            table.addCell(drug.getDrugName());
            table.addCell(drug.getDrugForm().name());
            table.addCell(drug.getExpirationDate().toLocalDate().toString());
        }
        return table;
    }

    /**
     * Generates a PDF document containing a list of drugs.
     *
     * @param drugs List of drugs to include in the PDF.
     * @return ByteArrayInputStream containing the generated PDF.
     */
    public ByteArrayInputStream generatePdf(List<DrugDTO> drugs) {

        log.info("Starting PDF generation for drug list");

        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            Paragraph title = new Paragraph("Drugs List", font);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(Chunk.NEWLINE);

            PdfPTable table = getPdfPTable(drugs);

            document.add(table);
            document.close();
            log.info("PDF generation completed successfully");

        } catch (Exception e) {
            log.error("Failed to generate PDF", e);
            throw new RuntimeException("Failed to generate PDF", e);
        }

        return new ByteArrayInputStream(out.toByteArray());
    }
}