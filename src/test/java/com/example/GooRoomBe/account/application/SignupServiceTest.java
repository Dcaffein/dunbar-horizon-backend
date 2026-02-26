package com.example.GooRoomBe.account.application;

import com.example.GooRoomBe.account.application.port.out.PasswordHasher;
import com.example.GooRoomBe.account.application.service.SignupService;
import com.example.GooRoomBe.account.domain.exception.AlreadyRegisteredEmailException;
import com.example.GooRoomBe.account.domain.model.Auth;
import com.example.GooRoomBe.account.domain.model.AuthProvider;
import com.example.GooRoomBe.account.domain.model.User;
import com.example.GooRoomBe.account.domain.model.UserStatus;
import com.example.GooRoomBe.account.domain.repository.AuthRepository;
import com.example.GooRoomBe.account.domain.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SignupServiceTest {

    @InjectMocks private SignupService signupService;

    @Mock private UserRepository userRepository;
    @Mock private AuthRepository authRepository;
    @Mock private PasswordHasher passwordHasher;
    @Mock private ApplicationEventPublisher eventPublisher;

    // ───────────────────────────── signup() ─────────────────────────────

    @Test
    @DisplayName("신규 유저 가입 시 유저를 생성하고 LOCAL auth를 저장한다")
    void signup_NewUser_CreatesUserAndLocalAuth() {
        String email = "test@test.com";
        given(userRepository.findByEmail(email)).willReturn(Optional.empty());
        given(userRepository.save(any(User.class))).willAnswer(inv -> {
            User u = inv.getArgument(0);
            ReflectionTestUtils.setField(u, "id", 1L);
            return u;
        });
        given(passwordHasher.encode("pw123")).willReturn("encoded-pw");
        given(authRepository.findByUserIdAndProvider(1L, AuthProvider.LOCAL)).willReturn(Optional.empty());

        signupService.signup(email, "pw123", "tester");

        verify(authRepository).save(any(Auth.class));
    }

    @Test
    @DisplayName("PENDING 유저 재가입 시 기존 미인증 auth의 비밀번호를 덮어쓴다")
    void signup_PendingUserWithExistingAuth_OverwritesPassword() {
        String email = "test@test.com";
        User pendingUser = User.builder().email(email).nickname("old").build();
        ReflectionTestUtils.setField(pendingUser, "id", 1L);
        Auth existingAuth = Auth.createLocalAuth(1L, "old-encoded-pw");

        given(userRepository.findByEmail(email)).willReturn(Optional.of(pendingUser));
        given(passwordHasher.encode("new-pw")).willReturn("new-encoded-pw");
        given(authRepository.findByUserIdAndProvider(1L, AuthProvider.LOCAL)).willReturn(Optional.of(existingAuth));

        signupService.signup(email, "new-pw", "newNickname");

        verify(authRepository).save(existingAuth);
        assertThat(existingAuth.getPassword()).isEqualTo("new-encoded-pw");
    }

    @Test
    @DisplayName("PENDING 유저 재가입 시 LOCAL auth가 없으면 새로 생성한다")
    void signup_PendingUserWithNoAuth_CreatesNewLocalAuth() {
        String email = "test@test.com";
        User pendingUser = User.builder().email(email).nickname("old").build();
        ReflectionTestUtils.setField(pendingUser, "id", 1L);

        given(userRepository.findByEmail(email)).willReturn(Optional.of(pendingUser));
        given(passwordHasher.encode("pw123")).willReturn("encoded-pw");
        given(authRepository.findByUserIdAndProvider(1L, AuthProvider.LOCAL)).willReturn(Optional.empty());

        signupService.signup(email, "pw123", "tester");

        verify(authRepository).save(any(Auth.class));
    }

    @Test
    @DisplayName("이미 활성화된 유저가 가입을 시도하면 예외를 던진다")
    void signup_ActiveUser_ThrowsException() {
        String email = "active@test.com";
        User activeUser = User.builder().email(email).nickname("active").status(UserStatus.ACTIVE).build();
        given(userRepository.findByEmail(email)).willReturn(Optional.of(activeUser));

        assertThatThrownBy(() -> signupService.signup(email, "pw", "active"))
                .isInstanceOf(AlreadyRegisteredEmailException.class);

        verify(authRepository, never()).save(any());
    }

    // ─────────────────────── registerOAuthUser() ────────────────────────

    @Test
    @DisplayName("신규 OAuth 유저는 ACTIVE 상태로 생성되고 UserActivatedEvent가 발행된다")
    void registerOAuthUser_NewUser_CreatesActiveUserAndPublishesEvent() {
        String email = "oauth@test.com";
        given(userRepository.findByEmail(email)).willReturn(Optional.empty());
        given(userRepository.save(any(User.class))).willAnswer(inv -> {
            User u = inv.getArgument(0);
            ReflectionTestUtils.setField(u, "id", 1L);
            return u;
        });
        given(authRepository.existsByUserIdAndProviderAndProviderId(1L, AuthProvider.GOOGLE, "google-id"))
                .willReturn(false);

        User result = signupService.registerOAuthUser(email, "oauthUser", AuthProvider.GOOGLE, "google-id");

        assertThat(result.getEmail()).isEqualTo(email);
        assertThat(result.isPending()).isFalse();
        verify(eventPublisher).publishEvent(any());
        verify(authRepository).save(any(Auth.class));
    }

    @Test
    @DisplayName("이메일 선점자(PENDING)가 있을 때 OAuth 가입 시 프로필을 덮어쓰고 활성화한다")
    void registerOAuthUser_PreemptedPendingUser_OverwritesAndActivates() {
        String email = "preempted@test.com";
        User pendingUser = User.builder().email(email).nickname("ghost").build();
        ReflectionTestUtils.setField(pendingUser, "id", 1L);

        given(userRepository.findByEmail(email)).willReturn(Optional.of(pendingUser));
        given(userRepository.save(pendingUser)).willReturn(pendingUser);
        given(authRepository.existsByUserIdAndProviderAndProviderId(1L, AuthProvider.GOOGLE, "google-id"))
                .willReturn(false);

        User result = signupService.registerOAuthUser(email, "realUser", AuthProvider.GOOGLE, "google-id");

        assertThat(result.isPending()).isFalse();
        assertThat(result.getNickname()).isEqualTo("realUser");
        verify(userRepository).save(pendingUser);
        verify(authRepository).save(any(Auth.class));
    }

    @Test
    @DisplayName("기존 활성 유저의 OAuth auth가 이미 존재하면 auth를 저장하지 않는다")
    void registerOAuthUser_ExistingOAuthAuth_SkipsAuthSave() {
        String email = "active@test.com";
        User activeUser = User.builder().email(email).nickname("active").status(UserStatus.ACTIVE).build();
        ReflectionTestUtils.setField(activeUser, "id", 1L);

        given(userRepository.findByEmail(email)).willReturn(Optional.of(activeUser));
        given(authRepository.existsByUserIdAndProviderAndProviderId(1L, AuthProvider.GOOGLE, "google-id"))
                .willReturn(true);

        signupService.registerOAuthUser(email, "active", AuthProvider.GOOGLE, "google-id");

        verify(authRepository, never()).save(any());
    }

    @Test
    @DisplayName("기존 활성 유저에게 새로운 OAuth provider가 연동되면 auth를 저장한다")
    void registerOAuthUser_NewOAuthProvider_SavesAuth() {
        String email = "active@test.com";
        User activeUser = User.builder().email(email).nickname("active").status(UserStatus.ACTIVE).build();
        ReflectionTestUtils.setField(activeUser, "id", 1L);

        given(userRepository.findByEmail(email)).willReturn(Optional.of(activeUser));
        given(authRepository.existsByUserIdAndProviderAndProviderId(1L, AuthProvider.GOOGLE, "google-id"))
                .willReturn(false);

        signupService.registerOAuthUser(email, "active", AuthProvider.GOOGLE, "google-id");

        verify(authRepository).save(any(Auth.class));
    }
}
