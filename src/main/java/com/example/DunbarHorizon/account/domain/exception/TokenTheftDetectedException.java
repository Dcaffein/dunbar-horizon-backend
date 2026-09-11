package com.example.DunbarHorizon.account.domain.exception;


public class TokenTheftDetectedException extends AccountException {
    public TokenTheftDetectedException() {
        super(AccountErrorCode.ACCOUNT_TOKEN_THEFT_DETECTED, "보안 위협이 감지되었습니다(토큰 재사용). 안전을 위해 다시 로그인해주세요.");
    }
}