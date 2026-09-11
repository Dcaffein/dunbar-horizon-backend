package com.example.DunbarHorizon.notification.domain.exception;


public class NotificationAccessDeniedException extends NotificationException {
    public NotificationAccessDeniedException(String message) {
        super(NotificationErrorCode.NOTIFICATION_ACCESS_DENIED, message);
    }
}
