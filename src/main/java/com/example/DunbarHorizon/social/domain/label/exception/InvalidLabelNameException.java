package com.example.DunbarHorizon.social.domain.label.exception;

import org.springframework.http.HttpStatus;

public class InvalidLabelNameException extends LabelException {
    public InvalidLabelNameException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
