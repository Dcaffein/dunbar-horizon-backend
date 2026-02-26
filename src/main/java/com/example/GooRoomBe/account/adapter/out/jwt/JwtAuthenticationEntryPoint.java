package com.example.GooRoomBe.account.adapter.out.jwt;

import com.example.GooRoomBe.global.exception.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        Exception exception = (Exception) request.getAttribute("exception");

        log.error("인증 실패 - 예외 타입: {}, 메시지: {}",
                exception != null ? exception.getClass().getSimpleName() : "null",
                authException.getMessage());

        String errorName = "UnAuthorizedException";
        String message = "인증되지 않은 사용자입니다.";

        if (exception instanceof ExpiredJwtException) {
            errorName = "TokenExpiredException";
            message = "토큰이 만료되었습니다.";
        } else if (exception instanceof MalformedJwtException || exception instanceof SecurityException) {
            errorName = "InvalidTokenException";
            message = "유효하지 않은 토큰 형식입니다.";
        }

        ErrorResponse errorResponse = ErrorResponse.builder()
                .error(errorName)
                .message(message)
                .build();

        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}