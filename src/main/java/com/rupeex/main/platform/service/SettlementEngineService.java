package com.rupeex.main.platform.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

@Service
public class SettlementEngineService {

    private final double failureRate;

    public SettlementEngineService(@Value("${payment.processing.failure-rate:0.2}") double failureRate) {
        this.failureRate = failureRate;
    }

    public boolean shouldFailThisAttempt() {
        return ThreadLocalRandom.current().nextDouble() < failureRate;
    }

    public long randomDelayMs() {
        return ThreadLocalRandom.current().nextLong(200L, 1800L);
    }
}
