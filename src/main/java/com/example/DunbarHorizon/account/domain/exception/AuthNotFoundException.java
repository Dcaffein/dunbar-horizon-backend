package com.example.DunbarHorizon.account.domain.exception;

import org.springframework.http.HttpStatus;

public class AuthNotFoundException extends AccountException {
  public AuthNotFoundException(String message) {
    super(message, HttpStatus.NOT_FOUND);
  }
}
