package com.rupeex.main.controller;

import com.rupeex.main.entity.FraudRule;
import com.rupeex.main.platform.dto.FraudRuleRequest;
import com.rupeex.main.platform.service.FraudRuleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/fraud/rules")
public class FraudRuleController {

    private final FraudRuleService fraudRuleService;

    public FraudRuleController(FraudRuleService fraudRuleService) {
        this.fraudRuleService = fraudRuleService;
    }

    @GetMapping
    public List<FraudRule> getRules() {
        return fraudRuleService.allRules();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FraudRule create(@Valid @RequestBody FraudRuleRequest request) {
        return fraudRuleService.create(request);
    }

    @PutMapping("/{id}")
    public FraudRule update(@PathVariable Long id, @Valid @RequestBody FraudRuleRequest request) {
        return fraudRuleService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        fraudRuleService.delete(id);
    }
}
