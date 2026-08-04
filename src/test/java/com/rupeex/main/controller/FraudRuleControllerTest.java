package com.rupeex.main.controller;

import com.rupeex.main.entity.FraudRule;
import com.rupeex.main.platform.dto.FraudRuleRequest;
import com.rupeex.main.platform.service.FraudRuleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = FraudRuleController.class)
@DisplayName("FraudRuleController Tests")
class FraudRuleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FraudRuleService fraudRuleService;

    @Test
    @DisplayName("Should get all fraud rules")
    void getRules_Success() throws Exception {
        List<FraudRule> rules = new ArrayList<>();
        rules.add(new FraudRule());
        when(fraudRuleService.allRules()).thenReturn(rules);

        mockMvc.perform(get("/fraud/rules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(fraudRuleService, times(1)).allRules();
    }

    @Test
    @DisplayName("Should return empty list when no rules")
    void getRules_Empty_Success() throws Exception {
        when(fraudRuleService.allRules()).thenReturn(new ArrayList<>());

        mockMvc.perform(get("/fraud/rules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Should create fraud rule")
    void create_ValidRequest_Returns201() throws Exception {
        FraudRule rule = new FraudRule();
        when(fraudRuleService.create(any(FraudRuleRequest.class))).thenReturn(rule);

        mockMvc.perform(post("/fraud/rules")
                .contentType("application/json")
                .content("{\"name\":\"Test Rule\",\"description\":\"Test description\",\"ruleType\":\"LARGE_TRANSACTION\",\"threshold\":1000,\"scoreContribution\":10,\"enabled\":true}"))
                .andExpect(status().isCreated());

        verify(fraudRuleService, times(1)).create(any());
    }

    @Test
    @DisplayName("Should update fraud rule")
    void update_ValidRequest_Returns200() throws Exception {
        FraudRule rule = new FraudRule();
        when(fraudRuleService.update(eq(1L), any(FraudRuleRequest.class))).thenReturn(rule);

        mockMvc.perform(put("/fraud/rules/1")
                .contentType("application/json")
                .content("{\"name\":\"Updated Rule\",\"description\":\"Updated description\",\"ruleType\":\"LARGE_TRANSACTION\",\"threshold\":2000,\"scoreContribution\":20,\"enabled\":true}"))
                .andExpect(status().isOk());

        verify(fraudRuleService, times(1)).update(eq(1L), any());
    }

    @Test
    @DisplayName("Should delete fraud rule")
    void delete_ValidId_Returns204() throws Exception {
        doNothing().when(fraudRuleService).delete(1L);

        mockMvc.perform(delete("/fraud/rules/1"))
                .andExpect(status().isNoContent());

        verify(fraudRuleService, times(1)).delete(1L);
    }

    @Test
    @DisplayName("Should return JSON content type")
    void getRules_CorrectContentType_Success() throws Exception {
        when(fraudRuleService.allRules()).thenReturn(new ArrayList<>());

        mockMvc.perform(get("/fraud/rules"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"));
    }
}

