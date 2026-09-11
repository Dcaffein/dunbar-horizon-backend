package com.example.DunbarHorizon.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;

/**
 * 컨트롤러를 거쳐 나온 예외의 출구.
 *
 * <p>시큐리티 필터 체인에서 나오는 예외는 여기까지 오지 않는다. 인증 실패는
 * {@code JwtAuthenticationEntryPoint}가 맡는다. 응답 형식을 바꿀 때는 두 곳을 함께 고쳐야 한다.
 *
 * <p>응답 {@code message}에 예외의 원본 메시지를 실어도 되는 것은 {@link BusinessException}뿐이다.
 * 그건 우리가 쓴 문장이기 때문이다. 그 외 예외는 우리가 새로 쓴 문구를 넣는다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        log.warn("{}: {}", e.getCode(), e.getMessage());

        ErrorResponse response = ErrorResponse.builder()
                .error(e.getCode())
                .message(e.getMessage())
                .build();

        return ResponseEntity
                .status(e.getHttpStatus())
                .body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("[Access Denied] {}", e.getMessage());

        return build(GlobalErrorCode.ACCESS_DENIED, "해당 리소스에 접근할 권한이 없습니다.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        log.warn("[Validation Exception] Input Value Invalid");

        Map<String, String> errors = new HashMap<>();
        for (FieldError error : e.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }

        ErrorResponse response = ErrorResponse.builder()
                .error(GlobalErrorCode.INVALID_INPUT.code())
                .message("입력값이 올바르지 않습니다.")
                .validation(errors)
                .build();

        return ResponseEntity
                .status(GlobalErrorCode.INVALID_INPUT.status())
                .body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleJsonException(HttpMessageNotReadableException e) {
        log.warn("[JSON Parse Exception] {}", e.getMessage());

        return build(GlobalErrorCode.INVALID_JSON_FORMAT, "요청 JSON 형식이 올바르지 않습니다.");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFoundException(NoResourceFoundException e) {
        return build(GlobalErrorCode.RESOURCE_NOT_FOUND,
                "요청하신 경로를 찾을 수 없습니다: " + e.getResourcePath());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return build(GlobalErrorCode.METHOD_NOT_ALLOWED,
                "지원하지 않는 요청 메서드입니다: " + e.getMethod());
    }

    @ExceptionHandler(ServletRequestBindingException.class)
    public ResponseEntity<ErrorResponse> handleRequestBinding(ServletRequestBindingException e) {
        log.warn("[Request Binding] {}", e.getMessage());

        return build(GlobalErrorCode.INVALID_REQUEST, "요청 파라미터가 올바르지 않습니다.");
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLockingFailureException(OptimisticLockingFailureException e) {
        log.warn("[Optimistic Locking Failure] {}", e.getMessage());

        return build(GlobalErrorCode.CONCURRENT_MODIFICATION,
                "다른 요청과 충돌이 발생했습니다. 잠시 후 다시 시도해주세요.");
    }

    /**
     * 예상하지 못한 예외를 전부 500 + 고정 문구로 덮는다. 스택트레이스·SQL·드라이버 메시지가
     * 응답으로 새지 않게 하는 안전장치이므로 원본은 로그로만 남긴다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("[Unhandled Exception] ", e);

        return build(GlobalErrorCode.INTERNAL_SERVER_ERROR,
                "서버 내부 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
    }

    private ResponseEntity<ErrorResponse> build(GlobalErrorCode errorCode, String message) {
        return ResponseEntity
                .status(errorCode.status())
                .body(ErrorResponse.builder()
                        .error(errorCode.code())
                        .message(message)
                        .build());
    }
}
