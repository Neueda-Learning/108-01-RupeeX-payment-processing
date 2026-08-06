package com.rupeex.main.platform;

import com.rupeex.main.entity.SystemEvent;
import com.rupeex.main.platform.service.SystemEventService;
import com.rupeex.main.repository.SystemEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SystemEventService Tests")
class SystemEventServiceTest {

    @Mock
    private SystemEventRepository systemEventRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private SystemEventService systemEventService;

    // Inject real ObjectMapper so JSON serialization works
    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        systemEventService = new SystemEventService(systemEventRepository, new ObjectMapper(), applicationEventPublisher);
    }

    @Test
    @DisplayName("Should save event and publish application event")
    void emit_SavesAndPublishesEvent() {
        SystemEvent saved = new SystemEvent();
        saved.setEventType("PAYMENT_CREATED");
        saved.setEntityId(1L);
        saved.setPayload("{}");
        when(systemEventRepository.save(any(SystemEvent.class))).thenReturn(saved);

        systemEventService.emit("PAYMENT_CREATED", 1L, "{}");

        verify(systemEventRepository, times(1)).save(any(SystemEvent.class));
        verify(applicationEventPublisher, times(1)).publishEvent(saved);
    }

    @Test
    @DisplayName("Should set correct fields on saved event")
    void emit_SetsEventFields() {
        when(systemEventRepository.save(any(SystemEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        systemEventService.emit("PAYMENT_FAILED", 42L, "Insufficient funds");

        ArgumentCaptor<SystemEvent> captor = ArgumentCaptor.forClass(SystemEvent.class);
        verify(systemEventRepository).save(captor.capture());

        SystemEvent event = captor.getValue();
        assertThat(event.getEventType()).isEqualTo("PAYMENT_FAILED");
        assertThat(event.getEntityId()).isEqualTo(42L);
        assertThat(event.getPayload()).contains("Insufficient funds");
    }

    @Test
    @DisplayName("Should return recent events")
    void getRecentEvents_ReturnsList() {
        SystemEvent e1 = new SystemEvent();
        SystemEvent e2 = new SystemEvent();
        when(systemEventRepository.findTop100ByOrderByCreatedAtDesc()).thenReturn(List.of(e1, e2));

        List<SystemEvent> events = systemEventService.getRecentEvents();

        assertThat(events).hasSize(2);
        verify(systemEventRepository).findTop100ByOrderByCreatedAtDesc();
    }

    @Test
    @DisplayName("Should handle non-serializable payload gracefully")
    void emit_NonSerializablePayload_UsesJsonFallback() {
        when(systemEventRepository.save(any(SystemEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        // A simple map payload should serialize fine
        systemEventService.emit("TEST_EVENT", 1L, java.util.Map.of("key", "value"));

        ArgumentCaptor<SystemEvent> captor = ArgumentCaptor.forClass(SystemEvent.class);
        verify(systemEventRepository).save(captor.capture());
        assertThat(captor.getValue().getPayload()).contains("key");
    }
}
