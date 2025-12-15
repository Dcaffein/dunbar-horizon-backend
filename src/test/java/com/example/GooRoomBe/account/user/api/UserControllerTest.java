package com.example.GooRoomBe.account.user.api;

import com.example.GooRoomBe.account.auth.application.LocalAccountService;
import com.example.GooRoomBe.account.user.api.dto.UserSignupRequestDto;
import com.example.GooRoomBe.support.ControllerTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest extends ControllerTestSupport {

    @MockitoBean
    private LocalAccountService localAccountService;

    @Test
    @DisplayName("회원가입: 유효한 정보로 요청 시 201 Created를 반환한다")
    void signup_Success() throws Exception {
        // Given
        UserSignupRequestDto requestDto = new UserSignupRequestDto(
                "test@email.com",
                "MyNickname",
                "Password123!"
        );

        // When & Then
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isCreated());

        // Verify
        verify(localAccountService).signUp(refEq(requestDto));
    }

    @Test
    @DisplayName("회원가입: 비밀번호 복잡도(영문+숫자+특수문자) 미충족 시 400 Bad Request를 반환한다")
    void signup_Fail_WeakPassword() throws Exception {
        // Given: 특수문자가 빠진 비밀번호
        UserSignupRequestDto requestDto = new UserSignupRequestDto(
                "test@email.com",
                "MyNickname",
                "Password123"
        );

        // When & Then
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isBadRequest()); // 400 기대
    }

    @Test
    @DisplayName("회원가입: 이메일 형식이 아니면 400 Bad Request를 반환한다")
    void signup_Fail_InvalidEmail() throws Exception {
        // Given (잘못된 이메일)
        UserSignupRequestDto requestDto = new UserSignupRequestDto(
                "not-email-format",
                "password123!",
                "nickname"
        );

        // When & Then
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("회원가입: 필수 값이 누락되면 400 Bad Request를 반환한다")
    void signup_Fail_EmptyField() throws Exception {
        UserSignupRequestDto requestDto = new UserSignupRequestDto(
                "test@email.com",
                "password123!",
                ""
        );

        // When & Then
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
}