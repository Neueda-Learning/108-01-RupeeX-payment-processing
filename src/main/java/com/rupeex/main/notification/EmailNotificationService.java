package com.rupeex.main.notification;

import com.rupeex.main.notification.model.NotificationRequest;
import com.rupeex.main.notification.template.TestEmailTemplateBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationService implements NotificationService {

    private final JavaMailSender javaMailSender;
    private final TestEmailTemplateBuilder templateBuilder;

    @Value("${notification.mail.from}")
    private String fromEmail;

    public EmailNotificationService(JavaMailSender javaMailSender, TestEmailTemplateBuilder templateBuilder) {
        this.javaMailSender = javaMailSender;
        this.templateBuilder = templateBuilder;
    }

    @Override
    @Async
    public void sendNotification(NotificationRequest request) {
        if (request == null || isBlank(request.getToEmail())) {
            return;
        }

        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setFrom(fromEmail);
        mailMessage.setTo(request.getToEmail());
        mailMessage.setSubject(isBlank(request.getSubject()) ? "[RupeeX Demo] Email Service Test" : request.getSubject());
        mailMessage.setText(templateBuilder.buildTestBody(request));

        javaMailSender.send(mailMessage);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

