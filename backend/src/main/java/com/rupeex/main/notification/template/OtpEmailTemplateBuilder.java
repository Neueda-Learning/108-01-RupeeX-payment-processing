package com.rupeex.main.notification.template;

import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Builds the plain-text email body for OTP notifications.
 */
@Component
public class OtpEmailTemplateBuilder {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm:ss z");

    /**
     * Builds the email body for a payment OTP.
     *
     * @param recipientName  Display name of the account holder (may be null/blank)
     * @param otp            The 4-digit OTP string
     * @param expiryMinutes  How many minutes until the OTP expires
     * @return Formatted plain-text email body
     */
    public String buildOtpBody(String recipientName, String otp, int expiryMinutes) {
        String name = (recipientName != null && !recipientName.isBlank()) ? recipientName : "Valued Customer";
        String timestamp = ZonedDateTime.now().format(FORMATTER);

        return "Dear " + name + ",\n\n"
                + "You have initiated a payment on the RupeeX platform.\n"
                + "To confirm this transaction, please use the One-Time Password (OTP) below:\n\n"
                + "    +-----------+\n"
                + "    |  " + otp + "   |\n"
                + "    +-----------+\n\n"
                + "This OTP is valid for " + expiryMinutes + " minute(s) from the time of this email.\n"
                + "Do NOT share this code with anyone — RupeeX will never ask for your OTP.\n\n"
                + "If you did NOT initiate this payment, please contact support immediately at support@rupeex.com.\n\n"
                + "Timestamp : " + timestamp + "\n\n"
                + "Regards,\n"
                + "RupeeX Security Team";
    }
}
