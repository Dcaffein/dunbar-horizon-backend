package com.example.DunbarHorizon.account.application.service;

import com.example.DunbarHorizon.account.application.port.in.UserQueryUseCase;
import com.example.DunbarHorizon.account.application.dto.UserProfileInfo;
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

    @Override
    public List<UserProfileInfo> getUserProfiles(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();

        return userRepository.findActivatedUsers(ids).stream()
                .map(UserProfileInfo::from)
                .toList();
    }

    @Override
    public Optional<UserProfileInfo> getActiveUserProfile(Long id) {
        return userRepository.findActivatedUser(id)
                .map(UserProfileInfo::from);
    }
}