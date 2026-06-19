package com.example.DunbarHorizon.buzz.domain.exception;

import org.springframework.http.HttpStatus;

public class BuzzNotFoundException extends BuzzException {
  public BuzzNotFoundException() {
    super("존재하지 않는 Buzz입니다.", HttpStatus.NOT_FOUND);
  }
}