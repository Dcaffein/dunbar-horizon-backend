package com.example.DunbarHorizon.account.application.service;

import com.example.DunbarHorizon.account.application.port.out.ProfileImageStoragePort;
import com.example.DunbarHorizon.account.domain.exception.UserNotFoundException;
import com.example.DunbarHorizon.account.domain.User;
import com.example.DunbarHorizon.account.domain.UserRole;
import com.example.DunbarHorizon.account.domain.UserStatus;
import com.example.DunbarHorizon.account.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

class UserProfileUpdateServiceTest {

    private UserRepository userRepository;
    private ProfileImageStoragePort profileImageStoragePort;
    private UserProfileUpdateService userProfileUpdateService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        profileImageStoragePort = mock(ProfileImageStoragePort.class);
        userProfileUpdateService = new UserProfileUpdateService(userRepository, profileImageStoragePort);
    }

    @Test
    @DisplayName("profileImageKey가 있으면 presigned GET URL을 조회하고 프로필을 업데이트한다")
    void updateProfile_withImageKey_resolvesUrlAndUpdates() {
        User user = spy(User.builder()
                .email("test@example.com")
                .nickname("기존닉네임")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(profileImageStoragePort.resolveUrl("profiles/uuid-photo")).willReturn("https://bucket.s3.amazonaws.com/profiles/uuid-photo?X-Amz-Signature=abc");

        userProfileUpdateService.updateProfile(1L, "새닉네임", "profiles/uuid-photo");

        verify(profileImageStoragePort).resolveUrl("profiles/uuid-photo");
        verify(user).updateProfile("새닉네임", "https://bucket.s3.amazonaws.com/profiles/uuid-photo?X-Amz-Signature=abc");
    }

    @Test
    @DisplayName("profileImageKey가 없으면 resolveUrl 호출 없이 null URL로 프로필을 업데이트한다")
    void updateProfile_withoutImageKey_updatesWithNullUrl() {
        User user = spy(User.builder()
                .email("test@example.com")
                .nickname("기존닉네임")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        userProfileUpdateService.updateProfile(1L, "새닉네임", null);

        verify(profileImageStoragePort, never()).resolveUrl(any());
        verify(user).updateProfile("새닉네임", null);
    }

    @Test
    @DisplayName("존재하지 않는 유저 ID면 UserNotFoundException을 던진다")
    void updateProfile_UserNotFound_ThrowsException() {
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userProfileUpdateService.updateProfile(99L, "닉네임", null))
                .isInstanceOf(UserNotFoundException.class);
    }
}
