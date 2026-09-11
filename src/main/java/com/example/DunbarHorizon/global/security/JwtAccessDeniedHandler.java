package com.example.DunbarHorizon.global.security;

import com.example.DunbarHorizon.global.exception.ErrorResponse;
import com.example.DunbarHorizon.global.exception.GlobalErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 시큐리티 필터 체인에서 나오는 인가 실패(403)의 출구.
 *
 * <p>인증(401)은 {@code JwtAuthenticationEntryPoint}가 {@code ErrorResponse}로 맞춰놨는데
 * 인가는 출구가 없어 컨테이너 기본 응답이 나갔다. 현재는 인가 규칙이
 * {@code anyRequest().authenticated()}뿐이라 발동 경로가 없지만, 역할 기반 규칙을 추가하는
 * 순간 형식이 어긋난다. 규칙보다 출구를 먼저 만들어 둔다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        log.warn("인가 실패 - uri: {}, 메시지: {}", request.getRequestURI(), accessDeniedException.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .error(GlobalErrorCode.ACCESS_DENIED.code())
                .message("해당 리소스에 접근할 권한이 없습니다.")
                .build();

        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
