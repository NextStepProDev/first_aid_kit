package com.firstaid.infrastructure.pdf;

import com.firstaid.controller.dto.DrugDTO;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfExportService {

    private static PdfPTable getPdfPTable(List<DrugDTO> drugs) {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD);
        table.addCell(new PdfPCell(new Phrase("LP", headerFont)));
        table.addCell(new PdfPCell(new Phrase("Name", headerFont)));
        table.addCell(new PdfPCell(new Phrase("Form", headerFont)));
        table.addCell(new PdfPCell(new Phrase("Expiration", headerFont)));

        int index = 1;
        for (DrugDTO drug : drugs) {
            table.addCell(String.valueOf(index++));
            table.addCell(drug.getDrugName());
            table.addCell(drug.getDrugForm().name());
            table.addCell(drug.getExpirationDate().toLocalDate().toString());
        }
        return table;
    }
    public byte[] generatePdf(List<DrugDTO> drugs) {

        log.info("Starting PDF generation for drug list");

        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font headerFont = FontFactory.getFont(FontFactory.TIMES_BOLDITALIC, 26, new Color(30, 144, 255));
            Paragraph header = new Paragraph("☤ First Aid Kit ☤", headerFont);
            header.setAlignment(Element.ALIGN_CENTER);
            header.setSpacingAfter(10f);
            document.add(header);

            LineSeparator separator = new LineSeparator();
            separator.setLineColor(new Color(30, 144, 255));
            separator.setPercentage(80f);
            document.add(separator);

            document.add(Chunk.NEWLINE);


            PdfPTable table = getPdfPTable(drugs);

            document.add(table);
            document.close();
            log.info("PDF generation completed successfully");

        } catch (Exception e) {
            log.error("Failed to generate PDF", e);
            throw new RuntimeException("Failed to generate PDF", e);
        }

        return out.toByteArray();
    }
}