package com.example.DunbarHorizon.notification.domain.exception;

import org.springframework.http.HttpStatus;

public class NotificationAccessDeniedException extends NotificationException {
    public NotificationAccessDeniedException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}
