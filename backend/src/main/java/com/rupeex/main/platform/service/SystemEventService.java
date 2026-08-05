package com.rupeex.main.platform.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rupeex.main.entity.SystemEvent;
import com.rupeex.main.repository.SystemEventRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SystemEventService {

    private final SystemEventRepository systemEventRepository;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher applicationEventPublisher;

    public SystemEventService(SystemEventRepository systemEventRepository,
                              ObjectMapper objectMapper,
                              ApplicationEventPublisher applicationEventPublisher) {
        this.systemEventRepository = systemEventRepository;
        this.objectMapper = objectMapper;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public void emit(String type, Long entityId, Object payload) {
        SystemEvent event = new SystemEvent();
        event.setEventType(type);
        event.setEntityId(entityId);
        event.setPayload(toJson(payload));
        SystemEvent saved = systemEventRepository.save(event);
        applicationEventPublisher.publishEvent(saved);
    }

    public List<SystemEvent> getRecentEvents() {
        return systemEventRepository.findTop100ByOrderByCreatedAtDesc();
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            return "{\"serialization\":\"failed\"}";
        }
    }
}
