package com.rupeex.main.notification;

import com.rupeex.main.notification.model.NotificationRequest;

public interface NotificationService {

    void sendNotification(NotificationRequest request);
}

