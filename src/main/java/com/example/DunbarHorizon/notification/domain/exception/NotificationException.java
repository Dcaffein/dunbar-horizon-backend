package com.example.DunbarHorizon.notification.domain.exception;


import com.example.DunbarHorizon.global.exception.ErrorContext;import com.example.DunbarHorizon.global.exception.BusinessException;
import com.example.DunbarHorizon.global.exception.ErrorCode;


public class NotificationException extends BusinessException {

    protected NotificationException(ErrorCode errorCode) {
        super(errorCode);
    }

    protected NotificationException(ErrorCode errorCode, ErrorContext context) {
        super(errorCode, context);
    }
}
