package com.rupeex.main.service;

import com.rupeex.main.dto.PaymentRequest;
import com.rupeex.main.service.model.TrustAssessmentResult;

public interface TrustScoreService {

    TrustAssessmentResult assessTrust(
            String customerId,
            PaymentRequest request
    );

    void applyVerificationOutcome(
            String customerId,
            TrustAssessmentResult assessmentResult,
            boolean approved
    );
}

