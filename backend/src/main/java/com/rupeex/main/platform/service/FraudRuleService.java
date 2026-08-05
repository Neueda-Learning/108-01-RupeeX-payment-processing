package com.rupeex.main.platform.service;

import com.rupeex.main.entity.FraudRule;
import com.rupeex.main.platform.dto.FraudRuleRequest;
import com.rupeex.main.repository.FraudRuleRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FraudRuleService {

    private final FraudRuleRepository fraudRuleRepository;

    public FraudRuleService(FraudRuleRepository fraudRuleRepository) {
        this.fraudRuleRepository = fraudRuleRepository;
    }

    public List<FraudRule> allRules() {
        return fraudRuleRepository.findAll();
    }

    public FraudRule create(FraudRuleRequest request) {
        FraudRule rule = new FraudRule();
        apply(rule, request);
        return fraudRuleRepository.save(rule);
    }

    public FraudRule update(Long id, FraudRuleRequest request) {
        FraudRule rule = fraudRuleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Rule not found: " + id));
        apply(rule, request);
        return fraudRuleRepository.save(rule);
    }

    public void delete(Long id) {
        fraudRuleRepository.deleteById(id);
    }

    private void apply(FraudRule rule, FraudRuleRequest request) {
        rule.setName(request.getName());
        rule.setDescription(request.getDescription());
        rule.setRuleType(request.getRuleType());
        rule.setThreshold(request.getThreshold());
        rule.setScoreContribution(request.getScoreContribution());
        rule.setEnabled(request.isEnabled());
    }
}
