package com.example.GooRoomBe.account.auth.application;

import com.example.GooRoomBe.account.auth.domain.LocalAuth;
import com.example.GooRoomBe.account.auth.domain.token.VerificationToken;
import com.example.GooRoomBe.account.auth.repository.LocalAuthRepository;
import com.example.GooRoomBe.account.auth.repository.VerificationTokenRepository;
import com.example.GooRoomBe.account.user.api.dto.UserSignupRequestDto;
import com.example.GooRoomBe.account.user.domain.User;
import com.example.GooRoomBe.account.user.domain.UserFactory;
import com.example.GooRoomBe.account.user.exception.UserNotFoundException;
import com.example.GooRoomBe.account.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LocalAccountServiceTest {

    @Mock UserRepository userRepository;
    @Mock LocalAuthRepository localAuthRepository;
    @Mock VerificationTokenRepository verificationTokenRepository;
    @Mock UserFactory userFactory;
    @Mock EmailService emailService;
    @Mock PasswordEncoder passwordEncoder;

    @InjectMocks LocalAccountService localAccountService;

    @Test
    @DisplayName("회원가입: 유저와 LocalAuth 엔티티가 저장되어야 한다")
    void signUp_Success() {
        // Given
        UserSignupRequestDto dto = new UserSignupRequestDto("test@email.com", "password", "nickname");
        User mockUser = mock(User.class);

        given(userFactory.createUnverifiedUser(dto.nickname(), dto.email())).willReturn(mockUser);
        given(passwordEncoder.encode(dto.password())).willReturn("encoded-password");

        // When
        localAccountService.signUp(dto);

        // Then
        // LocalAuth가 저장소에 save 되었는지 확인 (이때 비밀번호는 암호화되어 있어야 함)
        verify(localAuthRepository).save(any(LocalAuth.class));
    }

    @Test
    @DisplayName("인증 메일 발송: 유저가 존재하면 토큰을 생성하고 메일을 보낸다")
    void sendVerificationEmail_Success() {
        // Given
        String email = "exist@email.com";
        String redirectPage = "/home";
        User mockUser = mock(User.class);
        given(mockUser.getEmail()).willReturn(email);

        given(userRepository.findByEmail(email)).willReturn(Optional.of(mockUser));
        // 기존 토큰 삭제 로직을 위한 Mocking
        given(verificationTokenRepository.findByUser_Id(any())).willReturn(Optional.empty());

        // When
        localAccountService.sendVerificationEmail(email, redirectPage);

        // Then
        verify(verificationTokenRepository).save(any(VerificationToken.class)); // 새 토큰 저장 확인
        verify(emailService).sendVerificationEmail(eq(email), any(), eq(redirectPage)); // 메일 발송 확인
    }

    @Test
    @DisplayName("인증 메일 발송: 유저가 없으면 예외가 발생한다")
    void sendVerificationEmail_Fail_UserNotFound() {
        // Given
        given(userRepository.findByEmail(any())).willReturn(Optional.empty());

        // When & Then
        assertThrows(UserNotFoundException.class,
                () -> localAccountService.sendVerificationEmail("unknown@email.com", "/"));
    }

    @Test
    @DisplayName("이메일 인증: 유효한 토큰이면 유저 상태를 활성화(verify) 한다")
    void verifyEmail_Success() {
        // Given
        String tokenValue = "valid-token";
        VerificationToken mockToken = mock(VerificationToken.class);
        User mockUser = mock(User.class);

        given(verificationTokenRepository.findByTokenValue(tokenValue)).willReturn(Optional.of(mockToken));
        given(mockToken.getUser()).willReturn(mockUser);

        // When
        localAccountService.verifyEmail(tokenValue);

        // Then
        verify(mockToken).validateExpiration(); // 만료 검사 호출 확인
        verify(mockUser).verify();              // 유저 활성화 메서드 호출 확인
        verify(userRepository).save(mockUser);  // 유저 정보 업데이트 확인
        verify(verificationTokenRepository).delete(mockToken); // 사용된 토큰 삭제 확인
    }
}