package com.rupeex.main.service.model;

import com.rupeex.main.enums.TrustCategory;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public class TrustAssessmentResult {

    private final double trustScore;
    private final Set<TrustCategory> triggeredCategories;

    public TrustAssessmentResult(
            double trustScore,
            Set<TrustCategory> triggeredCategories
    ) {
        this.trustScore = trustScore;
        this.triggeredCategories = triggeredCategories.isEmpty()
                ? EnumSet.noneOf(TrustCategory.class)
                : EnumSet.copyOf(triggeredCategories);
    }

    public double getTrustScore() {
        return trustScore;
    }

    public Set<TrustCategory> getTriggeredCategories() {
        return Collections.unmodifiableSet(triggeredCategories);
    }

    public boolean hasTriggers() {
        return !triggeredCategories.isEmpty();
    }

    public boolean isTriggered(TrustCategory category) {
        return triggeredCategories.contains(category);
    }
}

