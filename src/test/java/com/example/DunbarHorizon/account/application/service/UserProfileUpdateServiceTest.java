package com.example.DunbarHorizon.account.application.service;

import com.example.DunbarHorizon.account.application.model.UploadFile;
import com.example.DunbarHorizon.account.application.port.out.ProfileImageStoragePort;
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
    private ProfileImageStoragePort profileImageStoragePort;
    private UserProfileUpdateService userProfileUpdateService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        profileImageStoragePort = mock(ProfileImageStoragePort.class);
        userProfileUpdateService = new UserProfileUpdateService(userRepository, profileImageStoragePort);
    }

    @Test
    @DisplayName("이미지 파일이 있으면 S3에 업로드하고 반환된 URL로 프로필을 업데이트한다")
    void updateProfile_withImage_uploadsAndUpdates() {
        User user = spy(User.builder()
                .email("test@example.com")
                .nickname("기존닉네임")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());
        UploadFile uploadFile = new UploadFile("bytes".getBytes(), "photo.jpg", "image/jpeg");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(profileImageStoragePort.upload(uploadFile)).willReturn("https://s3.example.com/profiles/photo.jpg");

        userProfileUpdateService.updateProfile(1L, "새닉네임", uploadFile);

        verify(profileImageStoragePort).upload(uploadFile);
        verify(user).updateProfile("새닉네임", "https://s3.example.com/profiles/photo.jpg");
    }

    @Test
    @DisplayName("이미지 파일이 없으면 S3 업로드 없이 null URL로 프로필을 업데이트한다")
    void updateProfile_withoutImage_updatesWithNullUrl() {
        User user = spy(User.builder()
                .email("test@example.com")
                .nickname("기존닉네임")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        userProfileUpdateService.updateProfile(1L, "새닉네임", null);

        verify(profileImageStoragePort, never()).upload(any());
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
