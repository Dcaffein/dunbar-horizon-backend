package com.example.GooRoomBe.account.auth.security.local.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class LoginFailureHandlerTest {

    @Spy
    ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    LoginFailureHandler loginFailureHandler;

    @Test
    @DisplayName("로그인 실패: BadCredentialsException 발생 시 401 상태와 에러 메시지를 반환한다")
    void onAuthenticationFailure() throws Exception {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        BadCredentialsException exception = new BadCredentialsException("비밀번호 틀림");

        // When
        loginFailureHandler.onAuthenticationFailure(request, response, exception);

        // Then
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());

        String responseBody = response.getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(responseBody);

        String message = jsonNode.get("message").asText();

        assertTrue(message.contains("일치하지 않습니다"));
    }
}