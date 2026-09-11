package com.example.DunbarHorizon.account.domain.exception;


public class AuthNotFoundException extends AccountException {
  public AuthNotFoundException(String message) {
    super(AccountErrorCode.ACCOUNT_AUTH_NOT_FOUND, message);
  }
}
