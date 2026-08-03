package com.rupeex.main.platform.service;

import com.rupeex.main.entity.FraudResult;
import java.util.List;

public record FraudEvaluationResult(int score, List<FraudResult> results, String explanation) {
}
