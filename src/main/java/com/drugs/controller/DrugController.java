package com.drugs.controller;

import com.drugs.controller.dto.*;
import com.drugs.infrastructure.pdf.PdfExportService;
import com.drugs.service.DrugService;
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
import org.springframework.validation.annotation.Validated;
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

    @GetMapping
    @Operation(summary = "Get all drugs", description = "Returns a list of all drugs in the database")
    @SuppressWarnings("unused")
    public List<DrugDTO> getAllDrugs() {
        log.info("Fetching all drugs from database");
        return drugService.getAllDrugs();
    }

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
    public List<String> getAvailableDrugForms() {
        log.info("Fetching available drug forms");
        return Arrays.stream(DrugFormDTO.values()).map(Enum::name).toList();
    }

    @GetMapping("/forms/dictionary")
    @Operation(summary = "Get drug form labels", description = "Returns a map of drug form enum values and their " +
            "labels")
    @SuppressWarnings("unused")
    public Map<String, String> getDrugsFormsDictionary() {
        log.info("Fetching drug form labels");
        return Arrays.stream(DrugFormDTO.values()).collect(Collectors.toMap(Enum::name, DrugFormDTO::getLabel));
    }

    @GetMapping("/by-name")
    @Operation(summary = "Get drugs by name", description = "Returns a list of drugs whose names match the given " +
            "value (case-insensitive)")
    @SuppressWarnings("unused")
    public ResponseEntity<List<DrugDTO>> getDrugsByName(@RequestParam String name) {
        log.info("Fetching drugs by name: {}", name);
        return ResponseEntity.ok(drugService.getDrugsByName(name));
    }

    @GetMapping("/expiration-until")
    @Validated
    @Operation(summary = "Get drugs expiring until", description = "Returns a list of drugs expiring until the " +
            "specified year and month")
    @SuppressWarnings("unused")
    public ResponseEntity<List<DrugDTO>> getDrugsExpiringUntil(
            @RequestParam @Min(1900) @Max(2100) int year,
            @RequestParam @Min(1) @Max(12) int month
    ) {
        log.info("Fetching drugs expiring until {}-{}", year, month);
        return ResponseEntity.ok(drugService.getDrugsExpiringSoon(year, month));
    }

    @GetMapping("/expired")
    @Operation(summary = "Get expired drugs", description = "Returns a list of drugs that have already expired")
    @SuppressWarnings("unused")
    public ResponseEntity<List<DrugDTO>> getExpiredDrugs() {
        log.info("Fetching expired drugs");
        return ResponseEntity.ok(drugService.getExpiredDrugs());
    }

    @GetMapping("/simple")
    @Operation(summary = "Get simple list of drugs", description = "Returns a simplified list of drugs containing " +
            "only " + "ID, name, form, and expiration")
    @SuppressWarnings("unused")
    public List<DrugSimpleDTO> getAllDrugsSimple() {
        log.info("Fetching simplified list of drugs");
        return drugService.getAllDrugsSimple();

    }
    @GetMapping("/paged")
    @Operation(summary = "Get drugs from pages", description = "Returns a list of drugs in the pages")
    @SuppressWarnings("unused")
    public Page<DrugDTO> getPagedDrugs(@ParameterObject Pageable pageable) {
        log.info("Fetching drugs with pagination");
        return drugService.getDrugsPaged(pageable);
    }

    @GetMapping("/by-form")
    @Operation(summary = "Get drugs by form", description = "Returns a list of drugs matching the given form")
    @SuppressWarnings("unused")
    public ResponseEntity<List<DrugDTO>> getDrugsByForm(@RequestParam String form) {
        log.info("Fetching drugs by form: {}", form);
        return ResponseEntity.ok(drugService.getDrugsByForm(form));
    }

    @GetMapping("/by-description")
    @Operation(summary = "Search by description", description = "Returns drugs whose descriptions contain given text" +
            " (case-insensitive)")
    @SuppressWarnings("unused")
    public ResponseEntity<List<DrugDTO>> searchByDescription(@RequestParam String description) {
        log.info("Searching drugs by description: {}", description);
        return ResponseEntity.ok(drugService.searchByDescription(description));
    }

    @GetMapping("/export/pdf")
    @Operation(summary = "Export drugs list to PDF")
    @SuppressWarnings("unused")
    public ResponseEntity<byte[]> exportDrugsToPdf() {
        log.info("Exporting drugs list to PDF");
        List<DrugDTO> drugs = drugService.getAllDrugs();
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

    @GetMapping("/sorted")
    @Operation(
            summary = "Get sorted drugs",
            description = "Returns a list of drugs sorted by the specified field. " +
                    "Example values: 'name', 'expirationDate', 'form', 'description'"
    )
    @SuppressWarnings("unused")
    public List<DrugDTO> getAllSorted(@RequestParam(defaultValue = "name") String sortBy) {
        log.info("Fetching drugs sorted by: {}", sortBy);
        return drugService.getAllSorted(sortBy);
    }
}