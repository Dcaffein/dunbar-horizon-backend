package com.example.DunbarHorizon.account.application;

import com.example.DunbarHorizon.account.application.dto.AuthTokenResult;
import com.example.DunbarHorizon.account.application.port.out.AuthTokenProvider;
import com.example.DunbarHorizon.account.application.port.out.PasswordHasher;
import com.example.DunbarHorizon.account.application.service.LoginService;
import com.example.DunbarHorizon.account.domain.*;
import com.example.DunbarHorizon.account.domain.repository.AuthRepository;
import com.example.DunbarHorizon.account.domain.repository.RefreshTokenRepository;
import com.example.DunbarHorizon.account.domain.repository.UserRepository;
import com.example.DunbarHorizon.global.event.DeviceTokenDeregisteredEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
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
    @Mock private ApplicationEventPublisher eventPublisher;

    @Test
    @DisplayName("로그인 시 비밀번호를 검증하고 토큰을 발급한다")
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

    @Test
    @DisplayName("로그아웃 시 refreshToken과 fcmToken이 모두 있으면 토큰 삭제 후 이벤트를 발행한다")
    void logout_refreshToken과_fcmToken이_있으면_삭제하고_이벤트_발행() {
        // when
        loginService.logout("refresh-token-value", "fcm-token-value");

        // then
        verify(refreshTokenRepository).deleteByTokenValue("refresh-token-value");

        ArgumentCaptor<DeviceTokenDeregisteredEvent> captor =
                ArgumentCaptor.forClass(DeviceTokenDeregisteredEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().fcmToken()).isEqualTo("fcm-token-value");
    }

    @Test
    @DisplayName("로그아웃 시 fcmToken이 없으면 refreshToken만 삭제하고 이벤트를 발행하지 않는다")
    void logout_fcmToken이_없으면_refreshToken만_삭제() {
        // when
        loginService.logout("refresh-token-value", null);

        // then
        verify(refreshTokenRepository).deleteByTokenValue("refresh-token-value");
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("로그아웃 시 refreshToken이 없어도 fcmToken이 있으면 이벤트를 발행한다")
    void logout_refreshToken이_없어도_fcmToken이_있으면_이벤트_발행() {
        // when
        loginService.logout(null, "fcm-token-value");

        // then
        verify(refreshTokenRepository, never()).deleteByTokenValue(any());
        verify(eventPublisher).publishEvent(any(DeviceTokenDeregisteredEvent.class));
    }
}
