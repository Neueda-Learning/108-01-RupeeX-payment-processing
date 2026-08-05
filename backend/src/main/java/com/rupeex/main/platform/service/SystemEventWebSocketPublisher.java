package com.rupeex.main.platform.service;

import com.rupeex.main.entity.SystemEvent;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class SystemEventWebSocketPublisher {

    private final SimpMessagingTemplate simpMessagingTemplate;

    public SystemEventWebSocketPublisher(SimpMessagingTemplate simpMessagingTemplate) {
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    @EventListener
    public void onSystemEvent(SystemEvent event) {
        simpMessagingTemplate.convertAndSend("/topic/events", event);
    }
}
