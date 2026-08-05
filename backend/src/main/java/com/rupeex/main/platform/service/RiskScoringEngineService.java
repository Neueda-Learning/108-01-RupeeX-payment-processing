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
        int bounded = Math.min(100, Math.max(0, fraudEvaluationResult.score()));
        RiskScore riskScore = new RiskScore();
        riskScore.setPaymentId(paymentId);
        riskScore.setScore(bounded);
        riskScore.setCategory(toCategory(bounded));
        riskScore.setExplanation(fraudEvaluationResult.explanation().isBlank() ? "No triggered rules" : fraudEvaluationResult.explanation());
        riskScore.setDecision(bounded >= 81 ? "Manual Review" : "Auto Process");
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
