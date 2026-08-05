package com.rupeex.main.controller;

import com.rupeex.main.entity.FraudRule;
import com.rupeex.main.platform.dto.FraudRuleRequest;
import com.rupeex.main.platform.service.FraudRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/fraud/rules")
@Tag(name = "Fraud Detection", description = "Fraud rules and detection management")
public class FraudRuleController {

    private final FraudRuleService fraudRuleService;

    public FraudRuleController(FraudRuleService fraudRuleService) {
        this.fraudRuleService = fraudRuleService;
    }

    @GetMapping
    @Operation(summary = "Get all fraud rules", description = "Retrieve all active fraud detection rules in the system")
    @ApiResponse(responseCode = "200", description = "Fraud rules retrieved successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = FraudRule.class)))
    public List<FraudRule> getRules() {
        return fraudRuleService.allRules();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create fraud rule", description = "Create a new fraud detection rule")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Fraud rule created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid fraud rule request")
    })
    public FraudRule create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Fraud rule details",
                    required = true)
            @Valid @RequestBody FraudRuleRequest request) {
        return fraudRuleService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update fraud rule", description = "Update an existing fraud detection rule")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Fraud rule updated successfully"),
            @ApiResponse(responseCode = "404", description = "Fraud rule not found"),
            @ApiResponse(responseCode = "400", description = "Invalid update request")
    })
    public FraudRule update(
            @Parameter(description = "Fraud rule ID", example = "1", required = true)
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Updated fraud rule details",
                    required = true)
            @Valid @RequestBody FraudRuleRequest request) {
        return fraudRuleService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete fraud rule", description = "Delete a fraud detection rule by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Fraud rule deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Fraud rule not found")
    })
    public void delete(
            @Parameter(description = "Fraud rule ID", example = "1", required = true)
            @PathVariable Long id) {
        fraudRuleService.delete(id);
    }
}
