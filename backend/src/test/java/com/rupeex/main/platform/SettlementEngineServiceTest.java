package com.rupeex.main.platform;

import com.rupeex.main.platform.service.SettlementEngineService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SettlementEngineService Tests")
class SettlementEngineServiceTest {

    @Test
    @DisplayName("Should return a delay within expected range")
    void randomDelayMs_ReturnsValueBetween200And1800() {
        SettlementEngineService service = new SettlementEngineService(0.2);

        long delay = service.randomDelayMs();

        assertThat(delay).isBetween(200L, 1800L);
    }

    @RepeatedTest(20)
    @DisplayName("randomDelayMs should always stay within bounds")
    void randomDelayMs_RepeatedCalls_AlwaysWithinBounds() {
        SettlementEngineService service = new SettlementEngineService(0.2);

        long delay = service.randomDelayMs();

        assertThat(delay).isGreaterThanOrEqualTo(200L).isLessThan(1800L);
    }

    @Test
    @DisplayName("shouldFailThisAttempt returns false when failure rate is 0")
    void shouldFailThisAttempt_ZeroRate_AlwaysReturnsFalse() {
        SettlementEngineService service = new SettlementEngineService(0.0);

        for (int i = 0; i < 50; i++) {
            assertThat(service.shouldFailThisAttempt()).isFalse();
        }
    }

    @Test
    @DisplayName("shouldFailThisAttempt returns true when failure rate is 1")
    void shouldFailThisAttempt_FullRate_AlwaysReturnsTrue() {
        SettlementEngineService service = new SettlementEngineService(1.0);

        for (int i = 0; i < 50; i++) {
            assertThat(service.shouldFailThisAttempt()).isTrue();
        }
    }

    @Test
    @DisplayName("shouldFailThisAttempt returns boolean with default rate")
    void shouldFailThisAttempt_DefaultRate_ReturnsBooleans() {
        SettlementEngineService service = new SettlementEngineService(0.2);

        // Just verify it returns a boolean without exception
        boolean result = service.shouldFailThisAttempt();
        assertThat(result).isIn(true, false);
    }
}
