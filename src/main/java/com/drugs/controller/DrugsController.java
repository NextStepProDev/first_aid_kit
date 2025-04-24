package com.drugs.controller;

import com.drugs.controller.dto.*;
import com.drugs.infrastructure.business.DrugsService;
import com.drugs.infrastructure.database.entity.DrugsEntity;
import com.drugs.infrastructure.database.repository.DrugsRepository;
import com.drugs.infrastructure.pdf.PdfExportService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

@RestController
@RequestMapping("/api/drugs")
@RequiredArgsConstructor
public class DrugsController {

    private final DrugsRepository drugsRepository;
    private final DrugsService drugsService;
    private final PdfExportService pdfExportService;

    private static final Logger logger = LoggerFactory.getLogger(DrugsController.class);

    @GetMapping
    @Operation(summary = "Get all drugs", description = "Returns a list of all drugs in the database")
    public List<DrugsEntity> getAllDrugs() {
        logger.info("Fetching all drugs from database");
        return drugsRepository.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get drug by ID", description = "Returns a drug by its ID or 404 if not found")
    public DrugsDTO getDrugById(@PathVariable Integer id) {
        logger.info("Fetching drug with ID: {}", id);
        return drugsService.getDrugById(id);
    }

    @PostMapping
    @Operation(summary = "Add new drug", description = "Adds a new drug to the database")
    public ResponseEntity<Void> addDrug(@RequestBody @Valid DrugsRequestDTO dto) {
        logger.info("Adding new drug with name: {}", dto.getName());
        drugsService.addNewDrug(dto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete drug by ID", description = "Deletes a drug from the database by its ID")
    public ResponseEntity<Void> deleteDrug(@PathVariable Integer id) {
        logger.info("Deleting drug with ID: {}", id);
        drugsService.deleteDrug(id);
        return ResponseEntity.noContent().build(); // 204 No Content
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update drug by ID", description = "Updates an existing drug using the given ID and data")
    public ResponseEntity<Void> updateDrug(@PathVariable Integer id, @Valid @RequestBody DrugsRequestDTO dto) {
        logger.info("Updating drug with ID: {}", id);
        try {
            drugsService.updateDrug(id, dto);
            logger.info("Drug with ID: {} updated successfully", id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            logger.error("Error updating drug with ID: {}", id, e);
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/forms")
    @Operation(summary = "Get drug forms", description = "Returns a list of all available drug forms (enum values)")
    public List<String> getAvailableDrugForms() {
        logger.info("Fetching available drug forms");
        return Arrays.stream(DrugsFormDTO.values()).map(Enum::name).toList();
    }

    @GetMapping("/forms/dictionary")
    @Operation(summary = "Get drug form labels", description = "Returns a map of drug form enum values and their labels")
    public Map<String, String> getDrugsFormsDictionary() {
        logger.info("Fetching drug form labels");
        return Arrays.stream(DrugsFormDTO.values()).collect(Collectors.toMap(Enum::name, DrugsFormDTO::getLabel));
    }

    @GetMapping("/by-name")
    @Operation(summary = "Get drugs by name", description = "Returns a list of drugs whose names match the given value (case-insensitive)")
    public ResponseEntity<List<DrugsDTO>> getDrugsByName(@RequestParam String name) {
        logger.info("Fetching drugs by name: {}", name);
        return ResponseEntity.ok(drugsService.getDrugsByName(name));
    }

    @GetMapping("/expiration-until")
    @Operation(summary = "Get drugs expiring until", description = "Returns a list of drugs expiring until the specified year and month")
    public ResponseEntity<List<DrugsDTO>> getDrugsExpiringUntil(
            @RequestParam int year,
            @RequestParam int month
    ) {
        logger.info("Fetching drugs expiring until {}-{}", year, month);
        return ResponseEntity.ok(drugsService.getDrugsExpiringSoon(year, month));
    }

    @GetMapping("/expired")
    @Operation(summary = "Get expired drugs", description = "Returns a list of drugs that have already expired")
    public ResponseEntity<List<DrugsDTO>> getExpiredDrugs() {
        logger.info("Fetching expired drugs");
        return ResponseEntity.ok(drugsService.getExpiredDrugs());
    }

    @GetMapping("/simple")
    @Operation(summary = "Get simple list of drugs", description = "Returns a simplified list of drugs containing only " +
            "ID, name, form, and expiration")
    public List<DrugSimpleDTO> getAllDrugsSimple() {
        logger.info("Fetching simplified list of drugs");
        return drugsService.getAllDrugsSimple();
    }

    @GetMapping("/paged")
    @Operation(summary = "Get drugs from pages", description = "Returns a list of drugs in the pages")
    public Page<DrugsDTO> getPagedDrugs(@ParameterObject Pageable pageable) {
        logger.info("Fetching drugs with pagination");
        return drugsService.getDrugsPaged(pageable);
    }

    @GetMapping("/by-description")
    @Operation(summary = "Search by description", description = "Returns drugs whose descriptions contain given text (case-insensitive)")
    public ResponseEntity<List<DrugsDTO>> searchByDescription(@RequestParam String description) {
        logger.info("Searching drugs by description: {}", description);
        return ResponseEntity.ok(drugsService.searchByDescription(description));
    }

    @GetMapping("/export/pdf")
    @Operation(summary = "Export drugs list to PDF")
    public ResponseEntity<byte[]> exportDrugsToPdf() {
        logger.info("Exporting drugs list to PDF");
        List<DrugsDTO> drugs = drugsService.getAllDrugs();
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
    public DrugStatisticsDTO getDrugStatistics() {
        logger.info("Fetching drug statistics");
        return drugsService.getDrugStatistics();
    }

    @GetMapping("/sorted")
    @Operation(
            summary = "Get sorted drugs",
            description = "Returns a list of drugs sorted by the specified field. " +
                    "Example values: 'drugsName', 'expirationDate', 'drugsForm'"
    )
    public List<DrugsDTO> getAllSorted(@RequestParam(defaultValue = "drugsName") String sortBy) {
        logger.info("Fetching drugs sorted by: {}", sortBy);
        return drugsService.getAllSorted(sortBy);
    }
}