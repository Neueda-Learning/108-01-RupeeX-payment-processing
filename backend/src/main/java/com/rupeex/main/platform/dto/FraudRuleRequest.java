package com.rupeex.main.platform.dto;

import com.rupeex.main.enums.FraudRuleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class FraudRuleRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String description;

    @NotNull
    private FraudRuleType ruleType;

    private double threshold;

    private int scoreContribution;

    private boolean enabled;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public FraudRuleType getRuleType() {
        return ruleType;
    }

    public void setRuleType(FraudRuleType ruleType) {
        this.ruleType = ruleType;
    }

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    public int getScoreContribution() {
        return scoreContribution;
    }

    public void setScoreContribution(int scoreContribution) {
        this.scoreContribution = scoreContribution;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
