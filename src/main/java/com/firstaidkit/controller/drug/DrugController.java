package com.firstaidkit.controller.drug;

import com.firstaidkit.controller.dto.drug.*;
import com.firstaidkit.infrastructure.csv.CsvExportService;
import com.firstaidkit.infrastructure.pdf.PdfExportService;
import com.firstaidkit.service.DrugService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Tag(name = "Drugs API")
@RestController
@RequestMapping("/api/drugs")
@RequiredArgsConstructor
@Validated
@SecurityRequirement(name = "bearerAuth")
public class DrugController {

    private final DrugService drugService;
    private final PdfExportService pdfExportService;
    private final CsvExportService csvExportService;

    private static final int MAX_SEARCH_PAGE_SIZE = 100;
    private static final int MAX_PDF_PAGE_SIZE = 1000;


    @GetMapping("/{id}")
    @Operation(summary = "Get drug by ID", description = "Returns a drug by its ID or 404 if not found")
    public DrugResponse getDrugById(@PathVariable @Min(value = 1, message = "ID must be >= 1") Integer id) {
        log.info("Fetching drug with ID: {}", id);
        return drugService.getDrugById(id);
    }

    @PostMapping
    @Operation(summary = "Add new drug", description = "Adds a new drug to the database")
    public ResponseEntity<DrugResponse> addDrug(@RequestBody @Valid DrugCreateRequest dto) {
        log.info("Adding new drug with name: {}", dto.getName());
        DrugResponse addedDrug = drugService.addNewDrug(dto);
        Integer id = (addedDrug != null) ? addedDrug.getDrugId() : null;
        log.info("Drug added successfully{}", id != null ? " with ID: " + id : "");
        return ResponseEntity.status(HttpStatus.CREATED).body(addedDrug);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete drug by ID", description = "Deletes a drug from the database by its ID")
    public ResponseEntity<Void> deleteDrug(@PathVariable Integer id) {
        log.info("Deleting drug with ID: {}", id);
        drugService.deleteDrug(id);
        return ResponseEntity.noContent().build(); // 204 No Content
    }

    @PostMapping("/delete-all")
    @Operation(
            summary = "Delete all drugs",
            description = "Permanently deletes all drugs for the current user. Requires password confirmation."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "All drugs deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Invalid password")
    })
    public ResponseEntity<Map<String, Long>> deleteAllDrugs(@Valid @RequestBody DeleteAllDrugsRequest request) {
        log.info("Request to delete all drugs");
        long deletedCount = drugService.deleteAllDrugs(request.password());
        log.info("Deleted {} drugs", deletedCount);
        return ResponseEntity.ok(Map.of("deletedCount", deletedCount));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update drug by ID", description = "Updates an existing drug using the given ID and data")
    public ResponseEntity<Void> updateDrug(@PathVariable Integer id, @Valid @RequestBody DrugCreateRequest dto) {
        log.info("Updating drug with ID: {}", id);
        drugService.updateDrug(id, dto);
        log.info("Drug with ID: {} updated successfully", id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/forms")
    @Operation(summary = "Get drug forms", description = "Returns a list of all available drug forms (enum values)")
    public List<FormOption> getAvailableDrugForms() {
        return Arrays.stream(DrugFormDTO.values())
                .map(f -> new FormOption(f.name().toLowerCase(), f.getLabel()))
                .toList();
    }

    @GetMapping("/forms/dictionary")
    @Operation(summary = "Get drug form labels", description = "Returns a map of drug form enum values and their " +
            "labels")
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
                    
                    ⚠️ PDF generation is limited to 100 records for performance reasons. Use filters to narrow down the dataset.
                    """
    )
    public ResponseEntity<byte[]> exportDrugsToPdf(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String form,
            @RequestParam(required = false) Boolean expired,
            @RequestParam(required = false) @Min(value = 2025, message = "Year must be >= 2025")
            @Max(value = 2100, message = "Year must be <= 2100") Integer expirationUntilYear,
            @RequestParam(required = false)
            @Min(value = 1, message = "Bro, months start from 1, not below.")
            @Max(value = 12, message = "Bro, months go only up to 12") Integer expirationUntilMonth,
            @ParameterObject Pageable pageable
    ) {
        if (pageable.getPageSize() > MAX_PDF_PAGE_SIZE) {
            throw new IllegalArgumentException("Maximum page size for PDF export is " + MAX_PDF_PAGE_SIZE + ".");
        }
        Page<DrugResponse> resultPage = drugService.searchDrugs(
                name, form, expired, null, expirationUntilYear, expirationUntilMonth, pageable
        );
        List<DrugResponse> drugs = resultPage.getContent();
        byte[] pdf = pdfExportService.generatePdf(drugs);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "inline; filename=drugs_list.pdf");

        return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/export/csv")
    @Operation(
            summary = "Export drugs list to CSV",
            description = """
                    Generates and returns a CSV file containing the list of drugs.
                    The file uses semicolon (;) as separator for Polish Excel compatibility.
                    📎 Use the URL under "Request URL" to download the file directly in your browser.
                    """
    )
    public ResponseEntity<byte[]> exportDrugsToCsv(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String form,
            @RequestParam(required = false) Boolean expired,
            @RequestParam(required = false) @Min(value = 2025, message = "Year must be >= 2025")
            @Max(value = 2100, message = "Year must be <= 2100") Integer expirationUntilYear,
            @RequestParam(required = false)
            @Min(value = 1, message = "Month must be between 1 and 12")
            @Max(value = 12, message = "Month must be between 1 and 12") Integer expirationUntilMonth,
            @ParameterObject Pageable pageable
    ) {
        if (pageable.getPageSize() > MAX_PDF_PAGE_SIZE) {
            throw new IllegalArgumentException("Maximum page size for CSV export is " + MAX_PDF_PAGE_SIZE + ".");
        }
        Page<DrugResponse> resultPage = drugService.searchDrugs(
                name, form, expired, null, expirationUntilYear, expirationUntilMonth, pageable
        );
        List<DrugResponse> drugs = resultPage.getContent();
        byte[] csv = csvExportService.generateCsv(drugs);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=drugs_list.csv");

        return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
                .body(csv);
    }

    @GetMapping("/statistics")
    @Operation(summary = "Retrieve drug statistics", description = "Returns statistics including total, expired, " +
            "active drugs, alerts sent, and a breakdown by form")
    public ResponseEntity<DrugStatistics> getDrugStatistics() {
        log.info("Fetching drug statistics");
        DrugStatistics stats = drugService.getDrugStatistics();
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
    - expiringSoon (true) - returns drugs expiring within next 30 days
    - expirationUntilYear & expirationUntilMonth (for expiration filtering)

    Supports sorting and pagination:
    - Available sort fields: drugName, expirationDate, drugForm.name
    - Sort format: sort=field,ASC|DESC (e.g., sort=expirationDate,DESC, sort=drugForm.name,drugName,asc)
    - Default page=0, size=20
    - Maximum page size for /search: 100
    """
    )
    public Page<DrugResponse> searchDrugs(
        @RequestParam(required = false) String name,
        @RequestParam(required = false) String form,
        @RequestParam(required = false) Boolean expired,
        @RequestParam(required = false) Boolean expiringSoon,
        @RequestParam(required = false) @Min(value = 2024, message = "Year must be >= 2024")
        @Max(value = 2100, message = "Year must be <= 2100") Integer expirationUntilYear,
        @RequestParam(required = false)
        @Min(value = 1, message = "Bro, months start from 1, not below.")
        @Max(value = 12, message = "Bro, months go only up to 12") Integer expirationUntilMonth,
        @ParameterObject Pageable pageable
    ) {
        log.info("Searching drugs with filters: name={}, form={}, expired={}, expiringSoon={}, expirationUntil={}-{}",
                name, form, expired, expiringSoon, expirationUntilYear, expirationUntilMonth);
        if (pageable.getPageSize() > MAX_SEARCH_PAGE_SIZE) {
            throw new IllegalArgumentException("Maximum page size exceeded. Allowed maximum is " + MAX_SEARCH_PAGE_SIZE);
        }
        return drugService.searchDrugs(name, form, expired, expiringSoon, expirationUntilYear, expirationUntilMonth, pageable);
    }
}