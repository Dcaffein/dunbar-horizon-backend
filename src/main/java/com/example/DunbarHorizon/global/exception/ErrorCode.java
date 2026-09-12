package com.example.DunbarHorizon.global.exception;

import org.springframework.http.HttpStatus;

/**
 * 에러 계약. 조건 하나에 코드 하나가 대응한다.
 *
 * <p>예외 클래스명에서 파생시키지 않고 상수로 명시한다. 클래스명을 파생시키면 이름을 바꾸는
 * 순간 API 계약이 바뀌는데, 컴파일러도 테스트도 diff도 그것을 잡지 못한다.
 *
 * <p>세 값의 청중이 다르다.
 *
 * <ul>
 *   <li>{@code status} — 브라우저·프록시·모니터링이 읽는다. 재시도할지, 장애로 집계할지.</li>
 *   <li>{@code code} — 클라이언트가 화면 분기에 쓴다. status보다 잘게 나뉜다.</li>
 *   <li>{@code message} — 사람이 읽는다. {@code code}를 풀어 쓴 설명이다.</li>
 * </ul>
 *
 * <p>같은 코드를 서로 다른 문장으로 던지고 싶어지면 <b>코드를 쪼갤 신호다.</b> 문장에만
 * 들어 있는 정보는 클라이언트가 분기에 쓸 수 없다.
 */
public interface ErrorCode {

    String code();

    HttpStatus status();

    /**
     * {@code code}가 가리키는 조건을 사람이 읽을 수 있게 풀어 쓴 설명.
     *
     * <p>문장에 값을 섞지 않는다. 값은 예외의 컨텍스트로 로그에 남는다.
     */
    String message();
}
