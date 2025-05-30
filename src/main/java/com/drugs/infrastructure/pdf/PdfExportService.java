package com.drugs.infrastructure.pdf;

import com.drugs.controller.dto.DrugsDTO;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PdfExportService {

    private static final Logger logger = LoggerFactory.getLogger(PdfExportService.class);

    public ByteArrayInputStream generatePdf(List<DrugsDTO> drugs) {
        logger.info("Starting PDF generation for drug list");

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

            PdfPTable table = new PdfPTable(4); // 4 kolumny
            table.setWidthPercentage(100);
            table.addCell("ID");
            table.addCell("Name");
            table.addCell("Form");
            table.addCell("Expiration");

            for (DrugsDTO drug : drugs) {
                table.addCell(String.valueOf(drug.getDrugsId()));
                table.addCell(drug.getDrugsName());
                table.addCell(drug.getDrugsForm().name());
                table.addCell(drug.getExpirationDate().toLocalDate().toString());
            }

            document.add(table);
            document.close();
            logger.info("PDF generation completed successfully");

        } catch (Exception e) {
            logger.error("Failed to generate PDF", e);
            throw new RuntimeException("Failed to generate PDF", e);
        }

        return new ByteArrayInputStream(out.toByteArray());
    }
}