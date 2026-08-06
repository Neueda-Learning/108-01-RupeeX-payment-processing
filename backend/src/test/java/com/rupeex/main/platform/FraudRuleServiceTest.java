package com.rupeex.main.platform;

import com.rupeex.main.entity.FraudRule;
import com.rupeex.main.enums.FraudRuleType;
import com.rupeex.main.platform.dto.FraudRuleRequest;
import com.rupeex.main.platform.service.FraudRuleService;
import com.rupeex.main.repository.FraudRuleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FraudRuleService Tests")
class FraudRuleServiceTest {

    @Mock
    private FraudRuleRepository fraudRuleRepository;

    @InjectMocks
    private FraudRuleService fraudRuleService;

    @Test
    @DisplayName("Should return all fraud rules")
    void allRules_ReturnsList() {
        FraudRule rule = new FraudRule();
        rule.setName("LARGE_TXN");
        when(fraudRuleRepository.findAll()).thenReturn(List.of(rule));

        List<FraudRule> result = fraudRuleService.allRules();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("LARGE_TXN");
    }

    @Test
    @DisplayName("Should create a new fraud rule")
    void create_ValidRequest_SavesRule() {
        FraudRuleRequest request = buildRequest("New Rule", FraudRuleType.LARGE_TRANSACTION, 10000, 40, true);

        FraudRule saved = new FraudRule();
        saved.setName("New Rule");
        when(fraudRuleRepository.save(any(FraudRule.class))).thenReturn(saved);

        FraudRule result = fraudRuleService.create(request);

        assertThat(result.getName()).isEqualTo("New Rule");
        verify(fraudRuleRepository, times(1)).save(any(FraudRule.class));
    }

    @Test
    @DisplayName("Should update existing fraud rule")
    void update_ExistingRule_UpdatesAndSaves() {
        FraudRule existing = new FraudRule();
        existing.setName("Old Name");
        when(fraudRuleRepository.findById(1L)).thenReturn(Optional.of(existing));

        FraudRuleRequest request = buildRequest("Updated Name", FraudRuleType.VELOCITY_CHECK, 5, 30, false);
        when(fraudRuleRepository.save(any(FraudRule.class))).thenAnswer(inv -> inv.getArgument(0));

        FraudRule result = fraudRuleService.update(1L, request);

        assertThat(result.getName()).isEqualTo("Updated Name");
        verify(fraudRuleRepository, times(1)).save(any(FraudRule.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when updating non-existent rule")
    void update_NonExistentRule_ThrowsException() {
        when(fraudRuleRepository.findById(99L)).thenReturn(Optional.empty());
        FraudRuleRequest request = buildRequest("Any", FraudRuleType.LARGE_TRANSACTION, 1000, 20, true);

        assertThatThrownBy(() -> fraudRuleService.update(99L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Rule not found: 99");
    }

    @Test
    @DisplayName("Should delete a fraud rule by id")
    void delete_CallsRepositoryDeleteById() {
        doNothing().when(fraudRuleRepository).deleteById(5L);

        fraudRuleService.delete(5L);

        verify(fraudRuleRepository, times(1)).deleteById(5L);
    }

    @Test
    @DisplayName("Should return empty list when no rules exist")
    void allRules_NoRules_ReturnsEmptyList() {
        when(fraudRuleRepository.findAll()).thenReturn(List.of());

        List<FraudRule> result = fraudRuleService.allRules();

        assertThat(result).isEmpty();
    }

    private FraudRuleRequest buildRequest(String name, FraudRuleType type, double threshold,
                                           int scoreContribution, boolean enabled) {
        FraudRuleRequest req = new FraudRuleRequest();
        req.setName(name);
        req.setDescription("Test description");
        req.setRuleType(type);
        req.setThreshold(threshold);
        req.setScoreContribution(scoreContribution);
        req.setEnabled(enabled);
        return req;
    }
}
