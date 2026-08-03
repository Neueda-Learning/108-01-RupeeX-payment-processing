package com.rupeex.main.platform.service;

import com.rupeex.main.entity.NotificationRecord;
import com.rupeex.main.repository.NotificationRecordRepository;
import org.springframework.stereotype.Service;

@Service
public class NotificationEngineService {

    private final NotificationRecordRepository notificationRecordRepository;
    private final SystemEventService systemEventService;

    public NotificationEngineService(NotificationRecordRepository notificationRecordRepository,
                                     SystemEventService systemEventService) {
        this.notificationRecordRepository = notificationRecordRepository;
        this.systemEventService = systemEventService;
    }

    public void notifyPaymentEvent(Long paymentId, String type, String payload) {
        NotificationRecord record = new NotificationRecord();
        record.setPaymentId(paymentId);
        record.setType(type);
        record.setPayload(payload);
        notificationRecordRepository.save(record);
        systemEventService.emit(type, paymentId, payload);
    }
}
