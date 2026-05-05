package com.example.DunbarHorizon.notification.domain.exception;

import com.example.DunbarHorizon.global.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class NotificationException extends BusinessException {
    public NotificationException(String message, HttpStatus status) {
        super(message, status);
    }
}
