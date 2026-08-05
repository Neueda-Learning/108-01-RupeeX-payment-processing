package com.rupeex.main.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("VerificationNotificationServiceImpl Tests")
class VerificationNotificationServiceImplTest {

    private VerificationNotificationServiceImpl verificationNotificationService;

    @BeforeEach
    void setUp() {
        verificationNotificationService = new VerificationNotificationServiceImpl();
    }

    @Test
    @DisplayName("Should send verification email")
    void sendVerificationEmail_ValidParams_Success() {
        // Given
        String customerEmail = "customer@example.com";
        Long paymentId = 1L;
        String verificationToken = "token-123";
        double trustScore = 0.95;

        // When & Then
        assertThatCode(() ->
                verificationNotificationService.sendVerificationEmail(customerEmail, paymentId, verificationToken, trustScore)
        ).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should handle null email gracefully")
    void sendVerificationEmail_NullEmail_Success() {
        assertThatCode(() ->
                verificationNotificationService.sendVerificationEmail(null, 1L, "token", 0.5)
        ).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should handle empty email gracefully")
    void sendVerificationEmail_EmptyEmail_Success() {
        assertThatCode(() ->
                verificationNotificationService.sendVerificationEmail("", 1L, "token", 0.5)
        ).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should handle blank email gracefully")
    void sendVerificationEmail_BlankEmail_Success() {
        assertThatCode(() ->
                verificationNotificationService.sendVerificationEmail("   ", 1L, "token", 0.5)
        ).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should handle high trust score")
    void sendVerificationEmail_HighTrustScore_Success() {
        assertThatCode(() ->
                verificationNotificationService.sendVerificationEmail("user@example.com", 1L, "token", 0.99)
        ).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should handle low trust score")
    void sendVerificationEmail_LowTrustScore_Success() {
        assertThatCode(() ->
                verificationNotificationService.sendVerificationEmail("user@example.com", 1L, "token", 0.1)
        ).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should handle zero trust score")
    void sendVerificationEmail_ZeroTrustScore_Success() {
        assertThatCode(() ->
                verificationNotificationService.sendVerificationEmail("user@example.com", 1L, "token", 0.0)
        ).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should handle maximum trust score")
    void sendVerificationEmail_MaxTrustScore_Success() {
        assertThatCode(() ->
                verificationNotificationService.sendVerificationEmail("user@example.com", 1L, "token", 1.0)
        ).doesNotThrowAnyException();
    }
}

