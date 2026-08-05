package com.rupeex.main.platform.service;

import com.rupeex.main.entity.RiskScore;
import com.rupeex.main.enums.RiskCategory;
import com.rupeex.main.repository.RiskScoreRepository;
import org.springframework.stereotype.Service;

@Service
public class RiskScoringEngineService {

    private final RiskScoreRepository riskScoreRepository;

    public RiskScoringEngineService(RiskScoreRepository riskScoreRepository) {
        this.riskScoreRepository = riskScoreRepository;
    }

    public RiskScore saveRiskScore(Long paymentId, FraudEvaluationResult fraudEvaluationResult) {
        int score = fraudEvaluationResult.score(); // Don't bound - allow scores > 100
        RiskScore riskScore = new RiskScore();
        riskScore.setPaymentId(paymentId);
        riskScore.setScore(score);
        riskScore.setCategory(toCategory(score));
        riskScore.setExplanation(fraudEvaluationResult.explanation().isBlank() ? "No triggered rules" : fraudEvaluationResult.explanation());
        
        // Decision logic based on risk score
        if (score > 100) {
            riskScore.setDecision("Auto Reject");
        } else if (score >= 80) {
            riskScore.setDecision("Admin Approval Required");
        } else {
            riskScore.setDecision("Auto Process");
        }
        
        return riskScoreRepository.save(riskScore);
    }

    private RiskCategory toCategory(int score) {
        if (score <= 30) {
            return RiskCategory.LOW;
        }
        if (score <= 60) {
            return RiskCategory.MEDIUM;
        }
        if (score <= 80) {
            return RiskCategory.HIGH;
        }
        return RiskCategory.CRITICAL;
    }
}
