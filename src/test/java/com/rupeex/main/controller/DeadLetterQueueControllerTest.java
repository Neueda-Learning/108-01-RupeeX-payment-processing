package com.rupeex.main.controller;

import com.rupeex.main.entity.DeadLetterQueueEntry;
import com.rupeex.main.repository.DeadLetterQueueRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DeadLetterQueueController.class)
@DisplayName("DeadLetterQueueController Tests")
class DeadLetterQueueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DeadLetterQueueRepository deadLetterQueueRepository;

    @Test
    @DisplayName("Should get all DLQ entries")
    void getAll_Success() throws Exception {
        List<DeadLetterQueueEntry> entries = new ArrayList<>();
        when(deadLetterQueueRepository.findAll()).thenReturn(entries);

        mockMvc.perform(get("/dlq"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(deadLetterQueueRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should return JSON content type")
    void getAll_CorrectContentType_Success() throws Exception {
        when(deadLetterQueueRepository.findAll()).thenReturn(new ArrayList<>());

        mockMvc.perform(get("/dlq"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"));
    }

    @Test
    @DisplayName("Should handle multiple DLQ entries")
    void getAll_Multiple_Success() throws Exception {
        List<DeadLetterQueueEntry> entries = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            entries.add(new DeadLetterQueueEntry());
        }
        when(deadLetterQueueRepository.findAll()).thenReturn(entries);

        mockMvc.perform(get("/dlq"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)));
    }
}

