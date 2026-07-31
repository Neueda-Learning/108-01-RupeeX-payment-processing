package com.rupeex.main.service;

public interface VerificationNotificationService {

    void sendVerificationEmail(
            String customerEmail,
            Long paymentId,
            String verificationToken,
            double trustScore
    );
}

