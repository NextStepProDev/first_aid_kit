package com.firstaidkit.unit.service;

import com.firstaidkit.infrastructure.email.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private Environment env;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(mailSender, env);
    }

    @Nested
    @DisplayName("sendEmail")
    class SendEmail {

        @Test
        @DisplayName("should send email with correct fields")
        void shouldSendEmailWithCorrectFields() {
            when(env.getProperty("spring.mail.username")).thenReturn("sender@gmail.com");

            emailService.sendEmail("recipient@example.com", "Test Subject", "Test Body");

            ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender).send(captor.capture());

            SimpleMailMessage sent = captor.getValue();
            assertThat(sent.getTo()).containsExactly("recipient@example.com");
            assertThat(sent.getSubject()).isEqualTo("Test Subject");
            assertThat(sent.getText()).isEqualTo("Test Body");
            assertThat(sent.getFrom()).isEqualTo("sender@gmail.com");
        }

        @Test
        @DisplayName("should propagate MailSendException from JavaMailSender")
        void shouldPropagateMailSendException() {
            when(env.getProperty("spring.mail.username")).thenReturn("sender@gmail.com");
            doThrow(new MailSendException("SMTP connection failed"))
                    .when(mailSender).send(any(SimpleMailMessage.class));

            assertThatThrownBy(() -> emailService.sendEmail("to@x.com", "Subject", "Body"))
                    .isInstanceOf(MailSendException.class)
                    .hasMessageContaining("SMTP connection failed");
        }
    }

    @Nested
    @DisplayName("sendEmailAsync")
    class SendEmailAsync {

        @Test
        @DisplayName("should send email with correct fields (without Spring proxy, executes synchronously)")
        void shouldSendEmailWithCorrectFields() {
            when(env.getProperty("spring.mail.username")).thenReturn("sender@gmail.com");

            emailService.sendEmailAsync("recipient@example.com", "Async Subject", "Async Body");

            ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender).send(captor.capture());

            SimpleMailMessage sent = captor.getValue();
            assertThat(sent.getTo()).containsExactly("recipient@example.com");
            assertThat(sent.getSubject()).isEqualTo("Async Subject");
            assertThat(sent.getText()).isEqualTo("Async Body");
            assertThat(sent.getFrom()).isEqualTo("sender@gmail.com");
        }

        @Test
        @DisplayName("should propagate exception when mail sending fails")
        void shouldPropagateExceptionWhenFails() {
            when(env.getProperty("spring.mail.username")).thenReturn("sender@gmail.com");
            doThrow(new MailSendException("SMTP auth failed"))
                    .when(mailSender).send(any(SimpleMailMessage.class));

            assertThatThrownBy(() -> emailService.sendEmailAsync("to@x.com", "Subject", "Body"))
                    .isInstanceOf(MailSendException.class);
        }
    }
}
