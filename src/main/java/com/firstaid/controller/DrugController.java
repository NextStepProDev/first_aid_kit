package com.firstaid.controller;

import com.firstaid.controller.dto.*;
import com.firstaid.infrastructure.pdf.PdfExportService;
import com.firstaid.service.DrugService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Tag(name = "Drugs API")
@RestController
@RequestMapping("/api/drugs")
@RequiredArgsConstructor
public class DrugController {

    private final DrugService drugService;
    private final PdfExportService pdfExportService;


    @GetMapping("/{id}")
    @Operation(summary = "Get drug by ID", description = "Returns a drug by its ID or 404 if not found")
    @SuppressWarnings("unused")
    public DrugDTO getDrugById(@PathVariable Integer id) {
        log.info("Fetching drug with ID: {}", id);
        return drugService.getDrugById(id);
    }

    @PostMapping
    @Operation(summary = "Add new drug", description = "Adds a new drug to the database")
    @SuppressWarnings("unused")
    public ResponseEntity<DrugDTO> addDrug(@RequestBody @Valid DrugRequestDTO dto) {
        log.info("Adding new drug with name: {}", dto.getName());
        DrugDTO addedDrug = drugService.addNewDrug(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(addedDrug);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete drug by ID", description = "Deletes a drug from the database by its ID")
    @SuppressWarnings("unused")
    public ResponseEntity<Void> deleteDrug(@PathVariable Integer id) {
        log.info("Deleting drug with ID: {}", id);
        drugService.deleteDrug(id);
        return ResponseEntity.noContent().build(); // 204 No Content
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update drug by ID", description = "Updates an existing drug using the given ID and data")
    @SuppressWarnings("unused")
    public ResponseEntity<Void> updateDrug(@PathVariable Integer id, @Valid @RequestBody DrugRequestDTO dto) {
        log.info("Updating drug with ID: {}", id);
        drugService.updateDrug(id, dto);
        log.info("Drug with ID: {} updated successfully", id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/forms")
    @Operation(summary = "Get drug forms", description = "Returns a list of all available drug forms (enum values)")
    @SuppressWarnings("unused")
    public List<FormOption> getAvailableDrugForms() {
        return Arrays.stream(DrugFormDTO.values())
                .map(f -> new FormOption(f.name().toLowerCase(), f.getLabel()))
                .toList();
    }

    @GetMapping("/forms/dictionary")
    @Operation(summary = "Get drug form labels", description = "Returns a map of drug form enum values and their " +
            "labels")
    @SuppressWarnings("unused")
    public Map<String, String> getDrugsFormsDictionary() {
        log.info("Fetching drug form labels");
        return Arrays.stream(DrugFormDTO.values()).collect(Collectors.toMap(Enum::name, DrugFormDTO::getLabel));
    }


    @GetMapping("/export/pdf")
    @Operation(
            summary = "Export drugs list to PDF",
            description = """
                    Generates and returns a PDF file containing the list of drugs.
                    📎 Use the URL under "Request URL" to download the file directly in your browser.
                    ⚠️ Swagger UI cannot display PDF properly.
                    
                    ⚠️ PDF generation is limited to 200 records for performance reasons. Use filters to narrow down the dataset.
                    """
    )
    @SuppressWarnings("unused")
    public ResponseEntity<byte[]> exportDrugsToPdf(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String form,
            @RequestParam(required = false) Boolean expired,
            @RequestParam(required = false) @Min(2024) @Max(2100) Integer expirationUntilYear,
            @RequestParam(required = false) @Min(1) @Max(12) Integer expirationUntilMonth,
            @ParameterObject Pageable pageable
    ) {
        if (pageable.getPageSize() > 500) {
            throw new IllegalArgumentException("Maximum page size for PDF export is 500.");
        }
        Page<DrugDTO> resultPage = drugService.searchDrugs(
                name, form, expired, expirationUntilYear, expirationUntilMonth, pageable
        );
        List<DrugDTO> drugs = resultPage.getContent();
        ByteArrayInputStream pdf = pdfExportService.generatePdf(drugs);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "inline; filename=drugs_list.pdf");

        return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf.readAllBytes());
    }

    @GetMapping("/statistics")
    @Operation(summary = "Retrieve drug statistics", description = "Returns statistics including total, expired, " +
            "active drugs, alerts sent, and a breakdown by form")
    @SuppressWarnings("unused")
    public ResponseEntity<DrugStatisticsDTO> getDrugStatistics() {
        log.info("Fetching drug statistics");
        DrugStatisticsDTO stats = drugService.getDrugStatistics();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/search")
    @Operation(
        summary = "Search drugs with filters, pagination and sorting",
        description = """
    Returns a paginated list of drugs filtered by optional parameters:
    - name (contains, case-insensitive)
    - form (e.g. pills, syrup)
    - expired (true/false)
    - expirationUntilYear & expirationUntilMonth (for expiration filtering)

    Supports sorting and pagination:
    - Available sort fields: drugName, expirationDate, drugForm.name
    - Sort format: sort=field,ASC|DESC (e.g., sort=expirationDate,DESC, sort=drugForm.name,drugName,asc)
    - Default page=0, size=20
    """
    )
    @SuppressWarnings("unused")
    public Page<DrugDTO> searchDrugs(
        @RequestParam(required = false) String name,
        @RequestParam(required = false) String form,
        @RequestParam(required = false) Boolean expired,
        @RequestParam(required = false) @Min(2024) @Max(2100) Integer expirationUntilYear,
        @RequestParam(required = false) @Min(1) @Max(12) Integer expirationUntilMonth,
        @ParameterObject Pageable pageable
    ) {
        log.info("Searching drugs with filters: name={}, form={}, expired={}, expirationUntil={}-{}",
                name, form, expired, expirationUntilYear, expirationUntilMonth);
        int maxPageSize = 100;
        if (pageable.getPageSize() > maxPageSize) {
            throw new IllegalArgumentException("Maximum page size exceeded. Allowed maximum is " + maxPageSize);
        }
        return drugService.searchDrugs(name, form, expired, expirationUntilYear, expirationUntilMonth, pageable);
    }
}