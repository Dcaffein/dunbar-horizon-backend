package com.example.DunbarHorizon.account.application;


import com.example.DunbarHorizon.account.application.dto.AuthTokenResult;
import com.example.DunbarHorizon.account.application.port.out.AuthTokenProvider;
import com.example.DunbarHorizon.account.application.port.out.PasswordHasher;
import com.example.DunbarHorizon.account.application.service.LoginService;
import com.example.DunbarHorizon.account.domain.model.*;
import com.example.DunbarHorizon.account.domain.repository.AuthRepository;
import com.example.DunbarHorizon.account.domain.repository.RefreshTokenRepository;
import com.example.DunbarHorizon.account.domain.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    @InjectMocks private LoginService loginService;

    @Mock private UserRepository userRepository;
    @Mock private AuthRepository authRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private AuthTokenProvider authTokenProvider;
    @Mock private PasswordHasher passwordHasher;

    @Test
    @DisplayName("로그인 시 Auth 엔티티의 비밀번호를 검증하고 토큰을 발급한다")
    void login_Success() {
        // given
        User user = User.builder().email("test@test.com").build();
        ReflectionTestUtils.setField(user, "id", 1L);
        Auth auth = spy(Auth.createLocalAuth(1L, "encoded-pw"));
        ReflectionTestUtils.setField(auth, "verified", true);

        given(userRepository.findByEmail(anyString())).willReturn(Optional.of(user));
        given(authRepository.findByUserIdAndProvider(1L, AuthProvider.LOCAL)).willReturn(Optional.of(auth));
        given(passwordHasher.matches(anyString(), anyString())).willReturn(true);
        given(authTokenProvider.createAccessToken(any())).willReturn("at");

        // when
        AuthTokenResult result = loginService.login("test@test.com", "password");

        // then
        assertThat(result.accessToken()).isEqualTo("at");
        verify(refreshTokenRepository).save(any());
    }
}