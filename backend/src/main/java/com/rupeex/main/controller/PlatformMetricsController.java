package com.rupeex.main.controller;

import com.rupeex.main.entity.SystemEvent;
import com.rupeex.main.platform.dto.MetricsSnapshotResponse;
import com.rupeex.main.platform.service.MetricsEngineService;
import com.rupeex.main.platform.service.SystemEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@Tag(name = "Platform Metrics", description = "System metrics and performance monitoring")
public class PlatformMetricsController {

    private final MetricsEngineService metricsEngineService;
    private final SystemEventService systemEventService;

    public PlatformMetricsController(MetricsEngineService metricsEngineService,
                                     SystemEventService systemEventService) {
        this.metricsEngineService = metricsEngineService;
        this.systemEventService = systemEventService;
    }

    @GetMapping("/metrics")
    @Operation(summary = "Get metrics snapshot", description = "Retrieve current system metrics including payment counts, success rates, and fraud indicators")
    @ApiResponse(responseCode = "200", description = "Metrics snapshot retrieved successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = MetricsSnapshotResponse.class)))
    public MetricsSnapshotResponse metrics() {
        return metricsEngineService.snapshot();
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Get dashboard summary", description = "Retrieve a comprehensive dashboard with key metrics including payments today, success/failure rates, fraud alerts, and average risk score")
    @ApiResponse(responseCode = "200", description = "Dashboard summary retrieved successfully")
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
    @Operation(summary = "Get recent system events", description = "Retrieve recent system events and notifications")
    @ApiResponse(responseCode = "200", description = "Recent events retrieved successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = SystemEvent.class)))
    public List<SystemEvent> events() {
        return systemEventService.getRecentEvents();
    }

    @GetMapping("/health")
    @Operation(summary = "Get service health status", description = "Check the health status of the payment processing service")
    @ApiResponse(responseCode = "200", description = "Service health status retrieved successfully")
    public Map<String, Object> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Payment Processing & Risk Intelligence Platform");
        return response;
    }
}
