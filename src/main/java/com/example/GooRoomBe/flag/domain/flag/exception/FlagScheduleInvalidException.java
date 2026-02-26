package com.example.GooRoomBe.flag.domain.flag.exception;

import org.springframework.http.HttpStatus;

public class FlagScheduleInvalidException extends FlagException {
  public FlagScheduleInvalidException(String message) {
    super(message, HttpStatus.CONFLICT);
  }
}
