package com.rupeex.main.controller;

import com.rupeex.main.entity.DeadLetterQueueEntry;
import com.rupeex.main.repository.DeadLetterQueueRepository;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/dlq")
public class DeadLetterQueueController {

    private final DeadLetterQueueRepository deadLetterQueueRepository;

    public DeadLetterQueueController(DeadLetterQueueRepository deadLetterQueueRepository) {
        this.deadLetterQueueRepository = deadLetterQueueRepository;
    }

    @GetMapping
    public List<DeadLetterQueueEntry> getAll() {
        return deadLetterQueueRepository.findAll();
    }
}
