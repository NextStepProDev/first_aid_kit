package com.firstaid.infrastructure.email;

import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final Environment env;

    @Async("emailTaskExecutor")
    @Retry(name = "emailService", fallbackMethod = "sendEmailFallback")
    public void sendEmailAsync(String to, String subject, String body) {
        sendEmail(to, subject, body);
    }

    public void sendEmail(String to, String subject, String body) {
        log.info("Preparing to send email to: {}, Subject: {}", to, subject);

        var message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);

        String fromPropertyName = "spring.mail.username";
        String from = env.getProperty(fromPropertyName);
        if (from == null || from.isBlank()) {
            throw new IllegalStateException("Missing required property: " + fromPropertyName);
        }

        message.setFrom(from);
        mailSender.send(message);
        log.info("Email successfully sent to: {}", to);
    }

    @SuppressWarnings("unused")
    private void sendEmailFallback(String to, String subject, String body, Exception e) {
        log.error("Failed to send email to {} after retries. Subject: {}. Error: {}", to, subject, e.getMessage());
    }
}
