package com.rupeex.main.notification.template;

import com.rupeex.main.notification.model.NotificationRequest;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
public class TestEmailTemplateBuilder {

    public String buildTestBody(NotificationRequest request) {
        String recipientName = isBlank(request.getRecipientName()) ? "User" : request.getRecipientName();
        String message = isBlank(request.getMessage())
                ? "Your notification service is working."
                : request.getMessage();

        StringBuilder body = new StringBuilder();
        body.append("Hi ").append(recipientName).append(",\n\n");
        body.append(message).append("\n\n");

        if (!isBlank(request.getReferenceId())) {
            body.append("Reference ID: ").append(request.getReferenceId()).append("\n");
        }

        body.append("Timestamp: ").append(OffsetDateTime.now()).append("\n\n");
        body.append("Regards,\nRupeeX");

        return body.toString();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

