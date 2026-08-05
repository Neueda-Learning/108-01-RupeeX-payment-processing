package com.rupeex.main.notification;

import com.rupeex.main.notification.model.NotificationRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
@DisplayName("NotificationController Tests")
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    @Test
    @DisplayName("Should send test notification")
    void sendTestNotification_ValidRequest_Returns202Accepted() throws Exception {
        doNothing().when(notificationService).sendNotification(any(NotificationRequest.class));

        mockMvc.perform(post("/notifications/test")
                .contentType("application/json")
                .content("{\"toEmail\":\"test@example.com\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("accepted"));

        verify(notificationService, times(1)).sendNotification(any());
    }

    @Test
    @DisplayName("Should reject notification with null email")
    void sendTestNotification_NullEmail_Returns400BadRequest() throws Exception {
        mockMvc.perform(post("/notifications/test")
                .contentType("application/json")
                .content("{\"toEmail\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("bad_request"));

        verify(notificationService, never()).sendNotification(any());
    }

    @Test
    @DisplayName("Should reject notification with empty email")
    void sendTestNotification_EmptyEmail_Returns400BadRequest() throws Exception {
        mockMvc.perform(post("/notifications/test")
                .contentType("application/json")
                .content("{\"toEmail\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("bad_request"));

        verify(notificationService, never()).sendNotification(any());
    }

    @Test
    @DisplayName("Should reject notification with blank email")
    void sendTestNotification_BlankEmail_Returns400BadRequest() throws Exception {
        mockMvc.perform(post("/notifications/test")
                .contentType("application/json")
                .content("{\"toEmail\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("bad_request"));

        verify(notificationService, never()).sendNotification(any());
    }

    @Test
    @DisplayName("Should send notification with valid email")
    void sendTestNotification_ValidEmail_Success() throws Exception {
        doNothing().when(notificationService).sendNotification(any(NotificationRequest.class));

        mockMvc.perform(post("/notifications/test")
                .contentType("application/json")
                .content("{\"toEmail\":\"user@example.com\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").value("Notification queued for async sending"));

        verify(notificationService, times(1)).sendNotification(any());
    }

    @Test
    @DisplayName("Should return JSON content type")
    void sendTestNotification_CorrectContentType_Success() throws Exception {
        doNothing().when(notificationService).sendNotification(any(NotificationRequest.class));

        mockMvc.perform(post("/notifications/test")
                .contentType("application/json")
                .content("{\"toEmail\":\"test@example.com\"}"))
                .andExpect(status().isAccepted())
                .andExpect(content().contentTypeCompatibleWith("application/json"));
    }
}

