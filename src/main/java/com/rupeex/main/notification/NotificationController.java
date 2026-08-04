package com.rupeex.main.notification;

import com.rupeex.main.notification.model.NotificationRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/notifications")
@Tag(name = "Notifications", description = "Notification management and delivery")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/test")
    @Operation(summary = "Send test notification", description = "Send a test notification to the specified email address")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Notification queued for async sending"),
            @ApiResponse(responseCode = "400", description = "Email address is required")
    })
    public ResponseEntity<Map<String, String>> sendTestNotification(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Notification request with email address",
                    required = true,
                    content = @Content(schema = @Schema(implementation = NotificationRequest.class)))
            @RequestBody NotificationRequest request) {
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

