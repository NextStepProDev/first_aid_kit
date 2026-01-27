package com.firstaidkit.controller.alert;

import com.firstaidkit.service.DrugService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/email")
@RequiredArgsConstructor
@Tag(name = "Alerts", description = "Drug expiry alert endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AlertController {

    private final DrugService drugService;

    @PostMapping("/alert")
    @Operation(summary = "Send expniry alerts", description = "Sends expiry alert emails for the current user's drugs expiring within the next month")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Alerts sent successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required")
    })
    public ResponseEntity<Integer> sendAlerts() {
        int count = drugService.sendDefaultExpiryAlertEmailsForCurrentUser();
        return ResponseEntity.ok(count);
    }
}