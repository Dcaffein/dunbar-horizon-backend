package com.example.DunbarHorizon.notification.domain.exception;

import com.example.DunbarHorizon.global.exception.BusinessException;
import com.example.DunbarHorizon.global.exception.ErrorCode;

public class NotificationException extends BusinessException {
    public NotificationException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
