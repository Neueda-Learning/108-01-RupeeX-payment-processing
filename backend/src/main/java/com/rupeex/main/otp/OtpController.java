package com.rupeex.main.otp;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/otp")
@Tag(name = "OTP", description = "Payment OTP generation and verification")
public class OtpController {

    private final OtpService otpService;

    public OtpController(OtpService otpService) {
        this.otpService = otpService;
    }

    @PostMapping("/send")
    @Operation(summary = "Send OTP", description = "Generates a 4-digit OTP and sends it to the email registered on the source account")
    public ResponseEntity<Void> sendOtp(@Valid @RequestBody OtpSendRequest request) {
        otpService.generateAndSend(request.getEmail(), request.getSourceAccount());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/verify")
    @Operation(summary = "Verify OTP", description = "Verifies the OTP entered by the user")
    public ResponseEntity<OtpVerifyResponse> verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        boolean valid = otpService.verify(request.getEmail(), request.getOtp());
        if (valid) {
            return ResponseEntity.ok(new OtpVerifyResponse(true, "OTP verified successfully"));
        }
        return ResponseEntity.ok(new OtpVerifyResponse(false, "Invalid or expired OTP"));
    }
}
