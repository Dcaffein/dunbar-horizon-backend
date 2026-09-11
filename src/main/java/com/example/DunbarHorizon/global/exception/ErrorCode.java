package com.example.DunbarHorizon.global.exception;

import org.springframework.http.HttpStatus;

/**
 * 에러 응답의 판별자.
 *
 * <p>예외 클래스명에서 파생시키지 않고 상수로 명시한다. 클래스명을 파생시키면
 * 이름을 바꾸는 순간 API 계약이 바뀌는데, 컴파일러도 테스트도 diff도 그것을 잡지 못한다.
 *
 * <p>{@code status}는 프로토콜 계층(브라우저, 프록시, 모니터링)이 읽고,
 * {@code code}는 클라이언트가 화면 분기에 쓴다. 둘을 한곳에 두어 어긋나지 않게 한다.
 */
public interface ErrorCode {

    String code();

    HttpStatus status();
}
