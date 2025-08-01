package com.drugs.controller.email;

import com.drugs.infrastructure.email.EmailService;
import com.drugs.service.DrugsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/email")
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class EmailController {

    private final DrugsService drugsService;
    private final EmailService emailService;

    @GetMapping("/alert")
    @SuppressWarnings("unused")
    public String sendExpiryAlerts() {
        drugsService.sendDefaultExpiryAlertEmails();

        log.info("Expiry alert emails have been sent.");

        return "Expiry alert emails have been sent";
    }
}