package com.example.GooRoomBe.cast.domain.exception;

import org.springframework.http.HttpStatus;

public class CastNotFoundException extends CastException {
  public CastNotFoundException() {
    super("존재하지 않는 캐스트입니다.", HttpStatus.NOT_FOUND);
  }
}