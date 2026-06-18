package com.example.DunbarHorizon.account.application.service;

import com.example.DunbarHorizon.account.application.port.in.UserQueryUseCase;
import com.example.DunbarHorizon.account.application.port.out.ProfileImageStoragePort;
import com.example.DunbarHorizon.account.application.dto.MyProfileResult;
import com.example.DunbarHorizon.account.application.dto.UserProfileInfo;
import com.example.DunbarHorizon.account.domain.UserStatus;
import com.example.DunbarHorizon.account.domain.exception.UserNotFoundException;
import com.example.DunbarHorizon.account.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserQueryService implements UserQueryUseCase {

    private final UserRepository userRepository;
    private final ProfileImageStoragePort profileImageStoragePort;

    @Override
    public List<UserProfileInfo> getUserProfiles(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();

        return userRepository.findActivatedUsers(ids).stream()
                .map(user -> new UserProfileInfo(user.getId(), user.getNickname(), resolveProfileUrl(user.getProfileImage())))
                .toList();
    }

    @Override
    public List<UserProfileInfo> getUserProfilesForSync(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();

        return userRepository.findActivatedUsers(ids).stream()
                .map(user -> new UserProfileInfo(user.getId(), user.getNickname(), user.getProfileImage()))
                .toList();
    }

    @Override
    public Optional<UserProfileInfo> getActiveUserProfile(Long id) {
        return userRepository.findActivatedUser(id)
                .map(user -> new UserProfileInfo(user.getId(), user.getNickname(), resolveProfileUrl(user.getProfileImage())));
    }

    @Override
    public Optional<UserProfileInfo> findActiveUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .map(user -> new UserProfileInfo(user.getId(), user.getNickname(), resolveProfileUrl(user.getProfileImage())));
    }

    @Override
    public MyProfileResult getMyProfile(Long id) {
        return userRepository.findActivatedUser(id)
                .map(user -> new MyProfileResult(user.getId(), user.getEmail(), user.getNickname(), resolveProfileUrl(user.getProfileImage())))
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));
    }

    private String resolveProfileUrl(String key) {
        return key != null ? profileImageStoragePort.resolveUrl(key) : null;
    }
}
