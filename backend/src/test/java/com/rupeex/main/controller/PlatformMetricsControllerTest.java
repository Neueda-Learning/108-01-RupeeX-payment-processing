package com.rupeex.main.controller;

import com.rupeex.main.platform.dto.MetricsSnapshotResponse;
import com.rupeex.main.platform.service.MetricsEngineService;
import com.rupeex.main.platform.service.SystemEventService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PlatformMetricsController.class)
@DisplayName("PlatformMetricsController Tests")
class PlatformMetricsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MetricsEngineService metricsEngineService;

    @MockBean
    private SystemEventService systemEventService;

    @Test
    @DisplayName("Should get metrics endpoint")
    void metrics_Success() throws Exception {
        MetricsSnapshotResponse response = new MetricsSnapshotResponse();
        when(metricsEngineService.snapshot()).thenReturn(response);

        mockMvc.perform(get("/metrics"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"));

        verify(metricsEngineService, times(1)).snapshot();
    }

    @Test
    @DisplayName("Should get dashboard endpoint")
    void dashboard_Success() throws Exception {
        MetricsSnapshotResponse response = new MetricsSnapshotResponse();
        response.setTotalPayments(100L);
        response.setSuccessRate(95.0);
        when(metricsEngineService.snapshot()).thenReturn(response);

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentsToday", is(100)));

        verify(metricsEngineService, times(1)).snapshot();
    }

    @Test
    @DisplayName("Should get system events")
    void events_Success() throws Exception {
        when(systemEventService.getRecentEvents()).thenReturn(new ArrayList<>());

        mockMvc.perform(get("/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(systemEventService, times(1)).getRecentEvents();
    }

    @Test
    @DisplayName("Should get health status")
    void health_Returns200() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")));
    }

    @Test
    @DisplayName("Should return JSON content type for metrics")
    void metrics_CorrectContentType_Success() throws Exception {
        MetricsSnapshotResponse response = new MetricsSnapshotResponse();
        when(metricsEngineService.snapshot()).thenReturn(response);

        mockMvc.perform(get("/metrics"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"));
    }

    @Test
    @DisplayName("Should handle zero payments dashboard")
    void dashboard_ZeroPayments_Success() throws Exception {
        MetricsSnapshotResponse response = new MetricsSnapshotResponse();
        response.setTotalPayments(0L);
        response.setSuccessRate(0.0);
        when(metricsEngineService.snapshot()).thenReturn(response);

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.failureRate", is(0.0)));
    }
}

