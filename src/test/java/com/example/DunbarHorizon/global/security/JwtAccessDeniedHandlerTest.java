package com.example.DunbarHorizon.global.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 인가 실패(403)의 출구.
 *
 * <p>현재 인가 규칙이 {@code anyRequest().authenticated()}뿐이라 실제 발동 경로가 없다.
 * 역할 기반 규칙을 추가하는 순간 이 핸들러가 응답을 만들게 되므로, 그때 형식이 어긋나지 않도록
 * 지금 고정해둔다. {@link JwtAuthenticationEntryPoint}와 같은 방식으로 직접 호출해 검증한다.
 */
class JwtAccessDeniedHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JwtAccessDeniedHandler handler = new JwtAccessDeniedHandler(objectMapper);

    @Test
    @DisplayName("인가 실패는 ACCESS_DENIED 코드와 함께 403을 응답한다")
    void handle_ReturnsAccessDenied() throws IOException {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/flags");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        handler.handle(request, response, new AccessDeniedException("Access Denied"));

        // then
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(body.get("error").asText()).isEqualTo("ACCESS_DENIED");
        assertThat(body.get("message").asText()).isEqualTo("해당 리소스에 접근할 권한이 없습니다.");
    }

    @Test
    @DisplayName("시큐리티 예외의 원본 메시지를 응답에 싣지 않는다")
    void handle_DoesNotLeakOriginalMessage() throws IOException {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/flags");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        handler.handle(request, response, new AccessDeniedException("내부 인가 규칙 세부사항"));

        // then
        assertThat(response.getContentAsString()).doesNotContain("내부 인가 규칙 세부사항");
    }
}
