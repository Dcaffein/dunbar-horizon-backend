package com.example.GooRoomBe.account.auth.security.core.exception;

import com.example.GooRoomBe.global.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        Object exceptionObj = request.getAttribute("exception");

        HttpStatus status = HttpStatus.UNAUTHORIZED;
        String message = "인증이 필요한 요청입니다.";

        if (exceptionObj instanceof BusinessException be) {
            status = be.getHttpStatus();
            message = be.getMessage();
        }

        response.setStatus(status.value());
        response.setContentType("application/json;charset=UTF-8");

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("error", status.getReasonPhrase());
        responseMap.put("message", message);

        response.getWriter().write(objectMapper.writeValueAsString(responseMap));
    }
}