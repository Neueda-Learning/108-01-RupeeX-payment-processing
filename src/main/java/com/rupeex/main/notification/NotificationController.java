package com.rupeex.main.notification;

import com.rupeex.main.notification.model.NotificationRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/test")
    public ResponseEntity<Map<String, String>> sendTestNotification(@RequestBody NotificationRequest request) {
        if (request == null || isBlank(request.getToEmail())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "bad_request",
                    "message", "toEmail is required"
            ));
        }

        notificationService.sendNotification(request);

        return ResponseEntity.accepted().body(Map.of(
                "status", "accepted",
                "message", "Notification queued for async sending"
        ));
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

