package com.example.DunbarHorizon.account.application.service;

import com.example.DunbarHorizon.account.domain.exception.UserNotFoundException;
import com.example.DunbarHorizon.account.domain.model.User;
import com.example.DunbarHorizon.account.domain.model.UserRole;
import com.example.DunbarHorizon.account.domain.model.UserStatus;
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
    @DisplayName("존재하는 유저의 프로필을 업데이트하면 updateProfile이 호출된다")
    void updateProfile_ExistingUser_CallsUpdateProfile() {
        // given
        User user = spy(User.builder()
                .email("test@example.com")
                .nickname("기존닉네임")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        // when
        userProfileUpdateService.updateProfile(1L, "새닉네임", "https://new.img");

        // then
        verify(user).updateProfile("새닉네임", "https://new.img");
    }

    @Test
    @DisplayName("존재하지 않는 유저 ID면 UserNotFoundException을 던진다")
    void updateProfile_UserNotFound_ThrowsException() {
        // given
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        // then
        assertThatThrownBy(() -> userProfileUpdateService.updateProfile(99L, "닉네임", null))
                .isInstanceOf(UserNotFoundException.class);
    }
}
