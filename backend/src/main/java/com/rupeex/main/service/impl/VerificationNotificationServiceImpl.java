package com.rupeex.main.service.impl;

import com.rupeex.main.service.VerificationNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class VerificationNotificationServiceImpl implements VerificationNotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(VerificationNotificationServiceImpl.class);

    @Override
    public void sendVerificationEmail(
            String customerEmail,
            Long paymentId,
            String verificationToken,
            double trustScore
    ) {
        if (customerEmail == null || customerEmail.isBlank()) {
            LOGGER.warn("Verification email skipped for payment {} because customer email is missing.", paymentId);
            return;
        }

        LOGGER.info(
                "Verification email simulated for paymentId={}, email={}, token={}, trustScore={}",
                paymentId,
                customerEmail,
                verificationToken,
                trustScore
        );
    }
}

