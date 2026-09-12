package com.example.DunbarHorizon.notification.domain.exception;

import com.example.DunbarHorizon.global.exception.ErrorContext;


public class NotificationAccessDeniedException extends NotificationException {

    public NotificationAccessDeniedException(String notificationId, Long userId) {
        super(NotificationErrorCode.NOTIFICATION_OWNER_ONLY, ErrorContext.of("notificationId", notificationId).and("userId", userId));
    }
}
