package com.example.DunbarHorizon.account.application;

import com.example.DunbarHorizon.account.application.port.out.EmailPort;
import com.example.DunbarHorizon.account.application.service.VerificationService;
import com.example.DunbarHorizon.account.domain.exception.AlreadyRegisteredEmailException;
import com.example.DunbarHorizon.account.domain.exception.VerificationTokenNotFoundException;
import com.example.DunbarHorizon.account.domain.Auth;
import com.example.DunbarHorizon.account.domain.AuthProvider;
import com.example.DunbarHorizon.account.domain.User;
import com.example.DunbarHorizon.account.domain.repository.AuthRepository;
import com.example.DunbarHorizon.account.domain.repository.EmailVerificationTokenRepository;
import com.example.DunbarHorizon.account.domain.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class VerificationServiceTest {

    @InjectMocks private VerificationService verificationService;

    @Mock private UserRepository userRepository;
    @Mock private AuthRepository authRepository;
    @Mock private EmailVerificationTokenRepository verificationTokenRepository;
    @Mock private EmailPort emailPort;

    @Test
    @DisplayName("인증 메일 발송 시 기존 토큰을 무효화하고 새 토큰을 저장 후 메일을 발송한다")
    void sendVerificationEmail_기존_토큰_무효화_후_재발송() {
        // given
        Long userId = 1L;
        User user = User.builder().email("test@test.com").build();
        ReflectionTestUtils.setField(user, "id", userId);
        Auth localAuth = Auth.createLocalAuth(userId, "pw");

        given(userRepository.findByEmail(anyString())).willReturn(Optional.of(user));
        given(authRepository.findByUserIdAndProvider(userId, AuthProvider.LOCAL)).willReturn(Optional.of(localAuth));

        // when
        verificationService.sendVerificationEmail("test@test.com", "redirect-url");

        // then
        verify(verificationTokenRepository).deleteByUserId(userId);
        verify(verificationTokenRepository).save(eq(userId), anyString());
        verify(emailPort).sendVerificationEmail(eq("test@test.com"), anyString(), eq("redirect-url"));
    }

    @Test
    @DisplayName("이미 인증된 이메일로 발송 요청 시 예외를 던진다")
    void sendVerificationEmail_이미_인증된_이메일_예외() {
        // given
        Long userId = 1L;
        User user = User.builder().email("test@test.com").build();
        ReflectionTestUtils.setField(user, "id", userId);
        Auth verifiedAuth = Auth.createLocalAuth(userId, "pw");
        verifiedAuth.verify();

        given(userRepository.findByEmail(anyString())).willReturn(Optional.of(user));
        given(authRepository.findByUserIdAndProvider(userId, AuthProvider.LOCAL)).willReturn(Optional.of(verifiedAuth));

        // when & then
        assertThatThrownBy(() -> verificationService.sendVerificationEmail("test@test.com", "redirect-url"))
                .isInstanceOf(AlreadyRegisteredEmailException.class);

        verify(verificationTokenRepository, never()).save(any(), any());
    }

    @Test
    @DisplayName("유효한 토큰으로 인증 시 Auth와 User를 활성화하고 토큰을 삭제한다")
    void verifyEmail_정상_인증_처리() {
        // given
        Long userId = 1L;
        String tokenStr = "valid-token";
        User user = User.builder().email("test@test.com").build();
        ReflectionTestUtils.setField(user, "id", userId);
        Auth localAuth = Auth.createLocalAuth(userId, "pw");

        given(verificationTokenRepository.findUserIdByToken(tokenStr)).willReturn(Optional.of(userId));
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(authRepository.findByUserIdAndProvider(userId, AuthProvider.LOCAL)).willReturn(Optional.of(localAuth));

        // when
        verificationService.verifyEmail(tokenStr);

        // then
        verify(authRepository).save(localAuth);
        verify(userRepository).save(user);
        verify(verificationTokenRepository).deleteByUserId(userId);
    }

    @Test
    @DisplayName("존재하지 않거나 만료된 토큰으로 인증 시 예외를 던진다")
    void verifyEmail_토큰_없음_예외() {
        // given
        given(verificationTokenRepository.findUserIdByToken(anyString())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> verificationService.verifyEmail("expired-or-invalid-token"))
                .isInstanceOf(VerificationTokenNotFoundException.class);
    }
}
