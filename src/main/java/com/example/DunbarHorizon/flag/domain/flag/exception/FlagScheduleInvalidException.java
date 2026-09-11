package com.example.DunbarHorizon.flag.domain.flag.exception;

import com.example.DunbarHorizon.flag.domain.exception.FlagErrorCode;

public class FlagScheduleInvalidException extends FlagException {
  public FlagScheduleInvalidException(String message) {
    super(FlagErrorCode.FLAG_SCHEDULE_INVALID, message);
  }
}
