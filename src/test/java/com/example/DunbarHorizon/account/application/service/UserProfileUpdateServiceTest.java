package com.example.DunbarHorizon.account.application.service;

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
    private UserProfileUpdateService userProfileUpdateService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userProfileUpdateService = new UserProfileUpdateService(userRepository);
    }

    @Test
    @DisplayName("profileImageKey가 있으면 key를 그대로 저장한다 (URL 변환은 조회 시점에 수행)")
    void updateProfile_withImageKey_storesKeyDirectly() {
        User user = spy(User.builder()
                .email("test@example.com")
                .nickname("기존닉네임")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        userProfileUpdateService.updateProfile(1L, "새닉네임", "profiles/uuid-photo");

        verify(user).updateProfile("새닉네임", "profiles/uuid-photo");
    }

    @Test
    @DisplayName("profileImageKey가 없으면 null로 프로필을 업데이트한다")
    void updateProfile_withoutImageKey_updatesWithNull() {
        User user = spy(User.builder()
                .email("test@example.com")
                .nickname("기존닉네임")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        userProfileUpdateService.updateProfile(1L, "새닉네임", null);

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
