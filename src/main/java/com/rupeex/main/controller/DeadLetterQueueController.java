package com.rupeex.main.controller;

import com.rupeex.main.entity.DeadLetterQueueEntry;
import com.rupeex.main.repository.DeadLetterQueueRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/dlq")
@Tag(name = "Dead Letter Queue", description = "Failed message handling and recovery")
public class DeadLetterQueueController {

    private final DeadLetterQueueRepository deadLetterQueueRepository;

    public DeadLetterQueueController(DeadLetterQueueRepository deadLetterQueueRepository) {
        this.deadLetterQueueRepository = deadLetterQueueRepository;
    }

    @GetMapping
    @Operation(summary = "Get all DLQ entries", description = "Retrieve all failed messages in the Dead Letter Queue that require manual intervention")
    @ApiResponse(responseCode = "200", description = "Dead letter queue entries retrieved successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = DeadLetterQueueEntry.class)))
    public List<DeadLetterQueueEntry> getAll() {
        return deadLetterQueueRepository.findAll();
    }
}
