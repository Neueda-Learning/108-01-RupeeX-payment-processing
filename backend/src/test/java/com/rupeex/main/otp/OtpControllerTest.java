package com.rupeex.main.otp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OtpController.class)
@DisplayName("OtpController Tests")
class OtpControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OtpService otpService;

    @Test
    @DisplayName("Should send OTP successfully")
    void sendOtp_ValidRequest_Returns200() throws Exception {
        doNothing().when(otpService).generateAndSend(anyString(), anyString());

        mockMvc.perform(post("/otp/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"sourceAccount\":\"ACC-001\"}"))
                .andExpect(status().isOk());

        verify(otpService, times(1)).generateAndSend("user@example.com", "ACC-001");
    }

    @Test
    @DisplayName("Should reject send OTP with blank email")
    void sendOtp_BlankEmail_Returns400() throws Exception {
        mockMvc.perform(post("/otp/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"\",\"sourceAccount\":\"ACC-001\"}"))
                .andExpect(status().isBadRequest());

        verify(otpService, never()).generateAndSend(anyString(), anyString());
    }

    @Test
    @DisplayName("Should reject send OTP with invalid email format")
    void sendOtp_InvalidEmailFormat_Returns400() throws Exception {
        mockMvc.perform(post("/otp/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"sourceAccount\":\"ACC-001\"}"))
                .andExpect(status().isBadRequest());

        verify(otpService, never()).generateAndSend(anyString(), anyString());
    }

    @Test
    @DisplayName("Should reject send OTP with blank sourceAccount")
    void sendOtp_BlankSourceAccount_Returns400() throws Exception {
        mockMvc.perform(post("/otp/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"sourceAccount\":\"\"}"))
                .andExpect(status().isBadRequest());

        verify(otpService, never()).generateAndSend(anyString(), anyString());
    }

    @Test
    @DisplayName("Should verify OTP successfully when valid")
    void verifyOtp_ValidOtp_ReturnsSuccess() throws Exception {
        when(otpService.verify("user@example.com", "1234")).thenReturn(true);

        mockMvc.perform(post("/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"otp\":\"1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.message").value("OTP verified successfully"));
    }

    @Test
    @DisplayName("Should return failure response when OTP is invalid")
    void verifyOtp_InvalidOtp_ReturnsFailure() throws Exception {
        when(otpService.verify("user@example.com", "9999")).thenReturn(false);

        mockMvc.perform(post("/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"otp\":\"9999\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.message").value("Invalid or expired OTP"));
    }

    @Test
    @DisplayName("Should reject verify OTP with non-4-digit OTP")
    void verifyOtp_NonFourDigitOtp_Returns400() throws Exception {
        mockMvc.perform(post("/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"otp\":\"12345\"}"))
                .andExpect(status().isBadRequest());

        verify(otpService, never()).verify(anyString(), anyString());
    }
}
