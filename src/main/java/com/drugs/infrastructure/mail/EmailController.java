package com.drugs.infrastructure.mail;

import com.drugs.infrastructure.business.DrugsService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/email")
@RequiredArgsConstructor
public class EmailController {

    private static final Logger logger = LoggerFactory.getLogger(EmailController.class);

    private final DrugsService drugsService;
    private final EmailService emailService;

    @GetMapping("/test")
    public String sendTestEmail() {
        emailService.sendEmail("djdefkon@gmail.com", "Test mail z aplikacji", "Działa! 🚀");
        emailService.sendEmail("paula.konarska@gmail.com", "Test mail z aplikacji", "Działa! 🚀");

        logger.info("Test emails sent to djdefkon@gmail.com and paula.konarska@gmail.com.");

        return "Mail wysłany";
    }

    @GetMapping("/alert")
    public String sendExpiryAlerts() {
        drugsService.sendDefaultExpiryAlertEmails();

        logger.info("Expiry alert emails have been sent.");

        return "Alerty zostały wysłane";
    }
}