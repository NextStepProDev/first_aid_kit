package com.drugs.infrastructure.mail;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    /**
     * Sends an email with the specified subject and body to the given recipient.
     *
     * @param to      The recipient's email address.
     * @param subject The subject of the email.
     * @param body    The body content of the email.
     */
    public void sendEmail(String to, String subject, String body) {
        logger.info("Preparing to send email to: {}, Subject: {}", to, subject);

        var message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        message.setFrom("djdefkon@gmail.com");

        try {
            mailSender.send(message);
            logger.info("Email successfully sent to: {}", to);
        } catch (Exception e) {
            logger.error("Failed to send email to: {}", to, e);
        }
    }
}