package com.drugs.infrastructure.email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    @SuppressWarnings("unused")
    private final Environment env;

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
}