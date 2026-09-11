package com.example.DunbarHorizon.buzz.domain.exception;


public class BuzzNotFoundException extends BuzzException {
  public BuzzNotFoundException() {
    super(BuzzErrorCode.BUZZ_NOT_FOUND, "존재하지 않는 Buzz입니다.");
  }
}