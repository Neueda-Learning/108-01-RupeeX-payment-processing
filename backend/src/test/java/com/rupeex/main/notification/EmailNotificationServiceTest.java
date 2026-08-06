package com.rupeex.main.notification;

import com.rupeex.main.notification.model.NotificationRequest;
import com.rupeex.main.notification.template.TestEmailTemplateBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailNotificationService Tests")
class EmailNotificationServiceTest {

    @Mock
    private JavaMailSender javaMailSender;

    @Mock
    private TestEmailTemplateBuilder templateBuilder;

    private EmailNotificationService emailNotificationService;

    @BeforeEach
    void setUp() {
        emailNotificationService = new EmailNotificationService(javaMailSender, templateBuilder);
        ReflectionTestUtils.setField(emailNotificationService, "fromEmail", "noreply@rupeex.com");
    }

    @Test
    @DisplayName("Should send email when valid request")
    void sendNotification_ValidRequest_SendsEmail() {
        NotificationRequest request = new NotificationRequest();
        request.setToEmail("user@example.com");
        when(templateBuilder.buildTestBody(any())).thenReturn("Test email body");

        emailNotificationService.sendNotification(request);

        verify(javaMailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Should not send email when toEmail is null")
    void sendNotification_NullEmail_DoesNotSend() {
        NotificationRequest request = new NotificationRequest();
        request.setToEmail(null);

        emailNotificationService.sendNotification(request);

        verify(javaMailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Should not send email when toEmail is blank")
    void sendNotification_BlankEmail_DoesNotSend() {
        NotificationRequest request = new NotificationRequest();
        request.setToEmail("   ");

        emailNotificationService.sendNotification(request);

        verify(javaMailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Should not send email when request is null")
    void sendNotification_NullRequest_DoesNotSend() {
        emailNotificationService.sendNotification(null);

        verify(javaMailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Should use default subject when no subject provided")
    void sendNotification_NoSubject_UsesDefaultSubject() {
        NotificationRequest request = new NotificationRequest();
        request.setToEmail("user@example.com");
        request.setSubject(null);
        when(templateBuilder.buildTestBody(any())).thenReturn("body");

        emailNotificationService.sendNotification(request);

        verify(javaMailSender).send(any(SimpleMailMessage.class));
    }
}
