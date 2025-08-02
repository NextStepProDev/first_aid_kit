package com.drugs.controller.alert;

import com.drugs.infrastructure.email.EmailService;
import com.drugs.service.DrugService;
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
@SuppressWarnings("unused")
public class AlertController {

    private final DrugService drugService;
    private final EmailService emailService;

    @PostMapping("/alerts")
    @SuppressWarnings("unused")
    public ResponseEntity<String> sendAlerts() {
        drugService.sendDefaultExpiryAlertEmails();
        return ResponseEntity.ok("Expiry alert emails have been sent");
    }
}