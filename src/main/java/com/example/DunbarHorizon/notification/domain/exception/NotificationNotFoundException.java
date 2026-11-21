package com.example.DunbarHorizon.notification.domain.exception;

import com.example.DunbarHorizon.global.exception.ErrorContext;

public class NotificationNotFoundException extends NotificationException {

    public NotificationNotFoundException(Long notificationId) {
        super(NotificationErrorCode.NOTIFICATION_NOT_FOUND, ErrorContext.of("notificationId", notificationId));
    }
}
