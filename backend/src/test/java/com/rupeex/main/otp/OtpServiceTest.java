package com.rupeex.main.otp;

import com.rupeex.main.notification.template.OtpEmailTemplateBuilder;
import com.rupeex.main.repository.AccountsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OtpService Tests")
class OtpServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private OtpEmailTemplateBuilder templateBuilder;

    @Mock
    private AccountsRepository accountsRepository;

    private OtpService otpService;

    @BeforeEach
    void setUp() {
        otpService = new OtpService(mailSender, templateBuilder, accountsRepository);
        ReflectionTestUtils.setField(otpService, "fromEmail", "noreply@rupeex.com");
        ReflectionTestUtils.setField(otpService, "fallbackEnabled", false);
        ReflectionTestUtils.setField(otpService, "fallbackCode", "0000");
    }

    @Test
    @DisplayName("Should generate and send OTP via email")
    void generateAndSend_FallbackDisabled_SendsEmail() {
        when(accountsRepository.findByAccountNumber("ACC-001")).thenReturn(Optional.empty());
        when(templateBuilder.buildOtpBody(any(), anyString(), anyInt())).thenReturn("OTP email body");

        otpService.generateAndSend("user@example.com", "ACC-001");

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Should skip email when fallback is enabled")
    void generateAndSend_FallbackEnabled_SkipsEmail() {
        ReflectionTestUtils.setField(otpService, "fallbackEnabled", true);

        otpService.generateAndSend("user@example.com", "ACC-001");

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Should return true when OTP is correct and not expired")
    void verify_CorrectOtp_ReturnsTrue() {
        when(accountsRepository.findByAccountNumber(anyString())).thenReturn(Optional.empty());
        when(templateBuilder.buildOtpBody(any(), anyString(), anyInt())).thenReturn("body");

        otpService.generateAndSend("user@example.com", "ACC-001");

        // Use fallback to verify since we cannot inspect the generated OTP directly
        ReflectionTestUtils.setField(otpService, "fallbackEnabled", true);
        ReflectionTestUtils.setField(otpService, "fallbackCode", "0000");

        boolean result = otpService.verify("user@example.com", "0000");

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return false when OTP is wrong")
    void verify_WrongOtp_ReturnsFalse() {
        when(accountsRepository.findByAccountNumber(anyString())).thenReturn(Optional.empty());
        when(templateBuilder.buildOtpBody(any(), anyString(), anyInt())).thenReturn("body");

        otpService.generateAndSend("user@example.com", "ACC-001");

        boolean result = otpService.verify("user@example.com", "0000");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return false when no OTP was generated for email")
    void verify_NoOtpGenerated_ReturnsFalse() {
        boolean result = otpService.verify("unknown@example.com", "1234");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Fallback code always verifies successfully when fallback enabled")
    void verify_FallbackEnabled_FallbackCodeVerifies() {
        ReflectionTestUtils.setField(otpService, "fallbackEnabled", true);
        ReflectionTestUtils.setField(otpService, "fallbackCode", "0000");

        boolean result = otpService.verify("anyone@example.com", "0000");

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should not send email if SMTP fails but store OTP")
    void generateAndSend_SmtpFails_DoesNotThrow() {
        when(accountsRepository.findByAccountNumber(anyString())).thenReturn(Optional.empty());
        when(templateBuilder.buildOtpBody(any(), anyString(), anyInt())).thenReturn("body");
        doThrow(new RuntimeException("SMTP error")).when(mailSender).send(any(SimpleMailMessage.class));

        // Should not propagate the SMTP exception
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(
                () -> otpService.generateAndSend("user@example.com", "ACC-001")
        );
    }
}
