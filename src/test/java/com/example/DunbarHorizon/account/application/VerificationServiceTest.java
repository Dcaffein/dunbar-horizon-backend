package com.example.DunbarHorizon.account.application;

import com.example.DunbarHorizon.account.application.port.out.EmailPort;
import com.example.DunbarHorizon.account.application.service.VerificationService;
import com.example.DunbarHorizon.account.domain.model.*;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class VerificationServiceTest {

    @InjectMocks private VerificationService verificationService;

    @Mock private UserRepository userRepository;
    @Mock private AuthRepository authRepository;
    @Mock private EmailVerificationTokenRepository verificationTokenRepository;
    @Mock private EmailPort emailPort;

    @Test
    @DisplayName("인증 메일 발송 시 미인증 상태여야 하며 기존 토큰을 삭제 후 재생성한다")
    void sendVerificationEmail_Success() {
        // given
        User user = User.builder().email("test@test.com").build();
        ReflectionTestUtils.setField(user, "id", 1L);
        Auth localAuth = Auth.createLocalAuth(1L, "pw"); // verified = false

        given(userRepository.findByEmail(anyString())).willReturn(Optional.of(user));
        given(authRepository.findByUserIdAndProvider(1L, AuthProvider.LOCAL)).willReturn(Optional.of(localAuth));

        // when
        verificationService.sendVerificationEmail("test@test.com", "redirect-url");

        // then
        verify(verificationTokenRepository).deleteByUser(user);
        verify(emailPort).sendVerificationEmail(eq("test@test.com"), anyString(), eq("redirect-url"));
    }
}