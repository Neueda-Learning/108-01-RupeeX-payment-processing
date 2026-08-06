package com.rupeex.main.platform;

import com.rupeex.main.entity.FraudResult;
import com.rupeex.main.entity.RiskScore;
import com.rupeex.main.enums.RiskCategory;
import com.rupeex.main.platform.service.FraudEvaluationResult;
import com.rupeex.main.platform.service.RiskScoringEngineService;
import com.rupeex.main.repository.RiskScoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RiskScoringEngineService Tests")
class RiskScoringEngineServiceTest {

    @Mock
    private RiskScoreRepository riskScoreRepository;

    @InjectMocks
    private RiskScoringEngineService riskScoringEngineService;

    @BeforeEach
    void setUp() {
        when(riskScoreRepository.save(any(RiskScore.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("Score 0 -> LOW risk, Auto Process")
    void saveRiskScore_ZeroScore_LowRiskAutoProcess() {
        FraudEvaluationResult evalResult = new FraudEvaluationResult(0, Collections.emptyList(), "");

        RiskScore saved = riskScoringEngineService.saveRiskScore(1L, evalResult);

        assertThat(saved.getCategory()).isEqualTo(RiskCategory.LOW);
        assertThat(saved.getDecision()).isEqualTo("Auto Process");
        assertThat(saved.getExplanation()).isEqualTo("No triggered rules");
    }

    @Test
    @DisplayName("Score 30 -> LOW risk, Auto Process")
    void saveRiskScore_Score30_LowRisk() {
        FraudEvaluationResult evalResult = new FraudEvaluationResult(30, Collections.emptyList(), "rule +30;");

        RiskScore saved = riskScoringEngineService.saveRiskScore(1L, evalResult);

        assertThat(saved.getCategory()).isEqualTo(RiskCategory.LOW);
        assertThat(saved.getDecision()).isEqualTo("Auto Process");
    }

    @Test
    @DisplayName("Score 31 -> MEDIUM risk, Auto Process")
    void saveRiskScore_Score31_MediumRisk() {
        FraudEvaluationResult evalResult = new FraudEvaluationResult(31, Collections.emptyList(), "rule +31;");

        RiskScore saved = riskScoringEngineService.saveRiskScore(1L, evalResult);

        assertThat(saved.getCategory()).isEqualTo(RiskCategory.MEDIUM);
        assertThat(saved.getDecision()).isEqualTo("Auto Process");
    }

    @Test
    @DisplayName("Score 60 -> MEDIUM risk, Auto Process")
    void saveRiskScore_Score60_MediumRisk() {
        FraudEvaluationResult evalResult = new FraudEvaluationResult(60, Collections.emptyList(), "medium +60;");

        RiskScore saved = riskScoringEngineService.saveRiskScore(1L, evalResult);

        assertThat(saved.getCategory()).isEqualTo(RiskCategory.MEDIUM);
    }

    @Test
    @DisplayName("Score 61 -> HIGH risk, Auto Process")
    void saveRiskScore_Score61_HighRisk() {
        FraudEvaluationResult evalResult = new FraudEvaluationResult(61, Collections.emptyList(), "high +61;");

        RiskScore saved = riskScoringEngineService.saveRiskScore(1L, evalResult);

        assertThat(saved.getCategory()).isEqualTo(RiskCategory.HIGH);
        assertThat(saved.getDecision()).isEqualTo("Auto Process");
    }

    @Test
    @DisplayName("Score 80 -> HIGH risk, Admin Approval Required")
    void saveRiskScore_Score80_AdminApproval() {
        FraudEvaluationResult evalResult = new FraudEvaluationResult(80, Collections.emptyList(), "high +80;");

        RiskScore saved = riskScoringEngineService.saveRiskScore(1L, evalResult);

        assertThat(saved.getCategory()).isEqualTo(RiskCategory.HIGH);
        assertThat(saved.getDecision()).isEqualTo("Admin Approval Required");
    }

    @Test
    @DisplayName("Score 81 -> CRITICAL risk, Admin Approval Required")
    void saveRiskScore_Score81_CriticalRisk() {
        FraudEvaluationResult evalResult = new FraudEvaluationResult(81, Collections.emptyList(), "critical +81;");

        RiskScore saved = riskScoringEngineService.saveRiskScore(1L, evalResult);

        assertThat(saved.getCategory()).isEqualTo(RiskCategory.CRITICAL);
        assertThat(saved.getDecision()).isEqualTo("Admin Approval Required");
    }

    @Test
    @DisplayName("Score > 100 -> CRITICAL risk, Auto Reject")
    void saveRiskScore_ScoreOver100_AutoReject() {
        FraudEvaluationResult evalResult = new FraudEvaluationResult(120, Collections.emptyList(), "critical rules;");

        RiskScore saved = riskScoringEngineService.saveRiskScore(1L, evalResult);

        assertThat(saved.getCategory()).isEqualTo(RiskCategory.CRITICAL);
        assertThat(saved.getDecision()).isEqualTo("Auto Reject");
    }

    @Test
    @DisplayName("Should set paymentId on saved risk score")
    void saveRiskScore_SetsPaymentId() {
        FraudEvaluationResult evalResult = new FraudEvaluationResult(10, Collections.emptyList(), "");

        RiskScore saved = riskScoringEngineService.saveRiskScore(42L, evalResult);

        assertThat(saved.getPaymentId()).isEqualTo(42L);
        verify(riskScoreRepository, times(1)).save(any(RiskScore.class));
    }
}
