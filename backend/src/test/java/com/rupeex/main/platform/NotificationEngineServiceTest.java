package com.rupeex.main.platform;

import com.rupeex.main.entity.NotificationRecord;
import com.rupeex.main.platform.service.NotificationEngineService;
import com.rupeex.main.platform.service.SystemEventService;
import com.rupeex.main.repository.NotificationRecordRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationEngineService Tests")
class NotificationEngineServiceTest {

    @Mock
    private NotificationRecordRepository notificationRecordRepository;

    @Mock
    private SystemEventService systemEventService;

    @InjectMocks
    private NotificationEngineService notificationEngineService;

    @Test
    @DisplayName("Should save notification record with correct fields")
    void notifyPaymentEvent_SavesNotificationRecord() {
        when(notificationRecordRepository.save(any(NotificationRecord.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        notificationEngineService.notifyPaymentEvent(1L, "PAYMENT_COMPLETED", "Success");

        ArgumentCaptor<NotificationRecord> captor = ArgumentCaptor.forClass(NotificationRecord.class);
        verify(notificationRecordRepository).save(captor.capture());

        NotificationRecord record = captor.getValue();
        assertThat(record.getPaymentId()).isEqualTo(1L);
        assertThat(record.getType()).isEqualTo("PAYMENT_COMPLETED");
        assertThat(record.getPayload()).isEqualTo("Success");
    }

    @Test
    @DisplayName("Should emit system event after saving record")
    void notifyPaymentEvent_EmitsSystemEvent() {
        when(notificationRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        notificationEngineService.notifyPaymentEvent(2L, "PAYMENT_FAILED", "Insufficient funds");

        verify(systemEventService).emit("PAYMENT_FAILED", 2L, "Insufficient funds");
    }

    @Test
    @DisplayName("Should handle null payload without NPE")
    void notifyPaymentEvent_NullPayload_UsesEmptyString() {
        when(notificationRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        notificationEngineService.notifyPaymentEvent(3L, "PAYMENT_RETRY", null);

        ArgumentCaptor<NotificationRecord> captor = ArgumentCaptor.forClass(NotificationRecord.class);
        verify(notificationRecordRepository).save(captor.capture());
        assertThat(captor.getValue().getPayload()).isEqualTo("");
    }

    @Test
    @DisplayName("Should emit system event with empty string when payload is null")
    void notifyPaymentEvent_NullPayload_EmitsWithEmptyPayload() {
        when(notificationRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        notificationEngineService.notifyPaymentEvent(4L, "DEBIT_POSTED", null);

        verify(systemEventService).emit("DEBIT_POSTED", 4L, "");
    }
}
