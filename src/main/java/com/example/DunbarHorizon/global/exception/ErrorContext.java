package com.example.DunbarHorizon.global.exception;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 예외가 로그에 남길 값. 재현과 조회에 필요한 식별자와 판정에 쓰인 값만 담는다.
 *
 * <pre>
 * ErrorContext.of("flagId", flagId).and("capacity", capacity)
 * </pre>
 *
 * <p>엔티티를 통째로 넣지 않는다. 비밀번호와 토큰 원문은 넣지 않는다.
 *
 * <p>{@code Map.of}를 쓰지 않는 이유는 null 값에서 NPE를 던지기 때문이다. 예외를 만들다가
 * 예외가 나면 원래 실패한 조건을 잃는다. 여기서는 null도 그대로 담아 기록한다.
 */
public final class ErrorContext {

    private static final ErrorContext EMPTY = new ErrorContext(Collections.emptyMap());

    private final Map<String, Object> values;

    private ErrorContext(Map<String, Object> values) {
        this.values = values;
    }

    public static ErrorContext none() {
        return EMPTY;
    }

    public static ErrorContext of(String key, Object value) {
        return EMPTY.and(key, value);
    }

    public ErrorContext and(String key, Object value) {
        Map<String, Object> next = new LinkedHashMap<>(values);
        next.put(key, value);
        return new ErrorContext(next);
    }

    boolean isEmpty() {
        return values.isEmpty();
    }

    /** 로그에 실리는 형태. {@code {flagId=331, capacity=8}} */
    @Override
    public String toString() {
        return values.toString();
    }
}
