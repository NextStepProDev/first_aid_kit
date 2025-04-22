package com.drugs.infrastructure.mail;

import com.drugs.infrastructure.business.DrugsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/email")
@RequiredArgsConstructor

public class EmailController {

    private final DrugsService drugsService;
    private final EmailService emailService;

    @GetMapping("/test")
    public String sendTestEmail() {
        emailService.sendEmail("djdefkon@gmail.com", "Test mail z aplikacji", "Działa! 🚀");
        emailService.sendEmail("paula.konarska@gmail.com", "Test mail z aplikacji", "Działa! 🚀");
        return "Mail wysłany";
    }

    @GetMapping("/alert")
    public String sendExpiryAlerts() {
        drugsService.sendExpiryAlertEmails();
        return "Alerty zostały wysłane";
    }
}