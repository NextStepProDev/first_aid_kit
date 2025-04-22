package com.drugs.controller;

import com.drugs.controller.dto.*;
import com.drugs.infrastructure.business.DrugsService;
import com.drugs.infrastructure.database.entity.DrugsEntity;
import com.drugs.infrastructure.database.repository.DrugsRepository;
import com.drugs.infrastructure.pdf.PdfExportService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

    @GetMapping
    @Operation(summary = "Get all drugs", description = "Returns a list of all drugs in the database")
    public List<DrugsEntity> getAllDrugs() {
        return drugsRepository.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get drug by ID", description = "Returns a drug by its ID or 404 if not found")
    public DrugsDTO getDrugById(@PathVariable Integer id) {
        return drugsService.getDrugById(id);
    }

    @PostMapping
    @Operation(summary = "Add new drug", description = "Adds a new drug to the database")
    public ResponseEntity<Void> addDrug(@RequestBody @Valid DrugsRequestDTO dto) {
        drugsService.addNewDrug(dto);
        return ResponseEntity.status(HttpStatus.CREATED).build();

        /*
        {
        "drugsName": "Ibuprofen",
        "drugsForm": "PILLS",
        "expirationYear": 2026,
        "expirationMonth": 6,
        "drugsDescription": "Przeciwbólowy i przeciwzapalny lek w formie tabletek."
        }
        */
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete drug by ID", description = "Deletes a drug from the database by its ID")
    public ResponseEntity<Void> deleteDrug(@PathVariable Integer id) {
        drugsService.deleteDrug(id);
        return ResponseEntity.noContent().build(); // 204 No Content
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update drug by ID", description = "Updates an existing drug using the given ID and data")
    public ResponseEntity<Void> updateDrug(@PathVariable Integer id, @Valid @RequestBody DrugsRequestDTO dto) {
        try {
            drugsService.updateDrug(id, dto);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/forms")
    @Operation(summary = "Get drug forms", description = "Returns a list of all available drug forms (enum values)")
    public List<String> getAvailableDrugForms() {
        return Arrays.stream(DrugsFormDTO.values()).map(Enum::name).toList();
    }

    @GetMapping("/forms/dictionary")
    @Operation(summary = "Get drug form labels", description = "Returns a map of drug form enum values and their labels")
    public Map<String, String> getDrugsFormsDictionary() {
        return Arrays.stream(DrugsFormDTO.values()).collect(Collectors.toMap(Enum::name, DrugsFormDTO::getLabel));
    }

    @GetMapping("/by-name")
    @Operation(summary = "Get drugs by name", description = "Returns a list of drugs whose names match the given value (case-insensitive)")
    public ResponseEntity<List<DrugsDTO>> getDrugsByName(@RequestParam String name) {
        return ResponseEntity.ok(drugsService.getDrugsByName(name));
    }

    @GetMapping("/expiration-until")
    @Operation(summary = "Get drugs expiring until", description = "Returns a list of drugs expiring until the specified year and month")
    public ResponseEntity<List<DrugsDTO>> getDrugsExpiringUntil(
            @RequestParam int year,
            @RequestParam int month
    ) {
        return ResponseEntity.ok(drugsService.getDrugsExpiringSoon(year, month));
    }

    @GetMapping("/expired")
    @Operation(summary = "Get expired drugs", description = "Returns a list of drugs that have already expired")
    public ResponseEntity<List<DrugsDTO>> getExpiredDrugs() {
        return ResponseEntity.ok(drugsService.getExpiredDrugs());
    }

    @GetMapping("/simple")
    @Operation(summary = "Get simple list of drugs", description = "Returns a simplified list of drugs containing only " +
            "ID, name, form, and expiration")
    public List<DrugSimpleDTO> getAllDrugsSimple() {
        return drugsService.getAllDrugsSimple();
    }

    @GetMapping("/paged")
    @Operation(summary = "Get drugs from pages", description = "Returns a list of drugs in the pages")
    public Page<DrugsDTO> getPagedDrugs(@ParameterObject Pageable pageable) {
        return drugsService.getDrugsPaged(pageable);
    }

    @GetMapping("/by-description")
    @Operation(summary = "Search by description", description = "Returns drugs whose descriptions contain given text (case-insensitive)")
    public ResponseEntity<List<DrugsDTO>> searchByDescription(@RequestParam String description) {
        return ResponseEntity.ok(drugsService.searchByDescription(description));
    }

    @GetMapping("/export/pdf")
    @Operation(summary = "Export drugs list to PDF")
    public ResponseEntity<byte[]> exportDrugsToPdf() {
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
        return drugsService.getDrugStatistics();
    }
}