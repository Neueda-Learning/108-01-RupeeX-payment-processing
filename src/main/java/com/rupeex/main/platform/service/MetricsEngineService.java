package com.rupeex.main.platform.service;

import com.rupeex.main.entity.Payment;
import com.rupeex.main.entity.PaymentMetric;
import com.rupeex.main.enums.PaymentStatus;
import com.rupeex.main.platform.dto.MetricsSnapshotResponse;
import com.rupeex.main.repository.FraudResultRepository;
import com.rupeex.main.repository.PaymentMetricRepository;
import com.rupeex.main.repository.PaymentRepository;
import com.rupeex.main.repository.ProcessingQueueRepository;
import com.rupeex.main.repository.RiskScoreRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MetricsEngineService {

    private final PaymentRepository paymentRepository;
    private final FraudResultRepository fraudResultRepository;
    private final ProcessingQueueRepository processingQueueRepository;
    private final RiskScoreRepository riskScoreRepository;
    private final PaymentMetricRepository paymentMetricRepository;

    public MetricsEngineService(PaymentRepository paymentRepository,
                                FraudResultRepository fraudResultRepository,
                                ProcessingQueueRepository processingQueueRepository,
                                RiskScoreRepository riskScoreRepository,
                                PaymentMetricRepository paymentMetricRepository) {
        this.paymentRepository = paymentRepository;
        this.fraudResultRepository = fraudResultRepository;
        this.processingQueueRepository = processingQueueRepository;
        this.riskScoreRepository = riskScoreRepository;
        this.paymentMetricRepository = paymentMetricRepository;
    }

    public MetricsSnapshotResponse snapshot() {
        long total = paymentRepository.count();
        long settled = paymentRepository.findByStatus(PaymentStatus.SETTLED).size()
                + paymentRepository.findByStatus(PaymentStatus.COMPLETED).size()
                + paymentRepository.findByStatus(PaymentStatus.SUCCESS).size();
        long failed = paymentRepository.findByStatus(PaymentStatus.FAILED).size();
        long queue = processingQueueRepository.countByStatus("READY") + processingQueueRepository.countByStatus("IN_PROGRESS");
        long fraudCount = fraudResultRepository.findAll().stream().filter(r -> r.isTriggered()).count();
        List<com.rupeex.main.entity.RiskScore> riskScores = riskScoreRepository.findAll();
        double avgRisk = riskScores.stream().mapToInt(com.rupeex.main.entity.RiskScore::getScore).average().orElse(0.0);

        MetricsSnapshotResponse response = new MetricsSnapshotResponse();
        response.setTotalPayments(total);
        response.setSuccessfulPayments(settled);
        response.setFailedPayments(failed);
        response.setQueueSize(queue);
        response.setFraudCount(fraudCount);
        response.setSuccessRate(total == 0 ? 0.0 : ((double) settled / total) * 100.0);
        response.setAverageRiskScore(avgRisk);

        persist("total_payments", total);
        persist("successful_payments", settled);
        persist("failed_payments", failed);
        persist("queue_size", queue);
        persist("fraud_count", fraudCount);
        persist("average_risk_score", avgRisk);

        return response;
    }

    private void persist(String metricName, double value) {
        PaymentMetric metric = new PaymentMetric();
        metric.setMetricName(metricName);
        metric.setMetricValue(value);
        paymentMetricRepository.save(metric);
    }
}
