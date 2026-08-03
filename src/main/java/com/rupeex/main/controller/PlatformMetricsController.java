package com.rupeex.main.controller;

import com.rupeex.main.entity.SystemEvent;
import com.rupeex.main.platform.dto.MetricsSnapshotResponse;
import com.rupeex.main.platform.service.MetricsEngineService;
import com.rupeex.main.platform.service.SystemEventService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class PlatformMetricsController {

    private final MetricsEngineService metricsEngineService;
    private final SystemEventService systemEventService;

    public PlatformMetricsController(MetricsEngineService metricsEngineService,
                                     SystemEventService systemEventService) {
        this.metricsEngineService = metricsEngineService;
        this.systemEventService = systemEventService;
    }

    @GetMapping("/metrics")
    public MetricsSnapshotResponse metrics() {
        return metricsEngineService.snapshot();
    }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard() {
        MetricsSnapshotResponse snapshot = metricsEngineService.snapshot();
        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("paymentsToday", snapshot.getTotalPayments());
        dashboard.put("successRate", snapshot.getSuccessRate());
        dashboard.put("failureRate", snapshot.getTotalPayments() == 0 ? 0.0 : (100.0 - snapshot.getSuccessRate()));
        dashboard.put("fraudAlerts", snapshot.getFraudCount());
        dashboard.put("queueLength", snapshot.getQueueSize());
        dashboard.put("averageRiskScore", snapshot.getAverageRiskScore());
        return dashboard;
    }

    @GetMapping("/events")
    public List<SystemEvent> events() {
        return systemEventService.getRecentEvents();
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Payment Processing & Risk Intelligence Platform");
        return response;
    }
}
