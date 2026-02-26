package com.example.GooRoomBe.account.application.service;

import com.example.GooRoomBe.account.application.port.in.UserQueryUseCase;
import com.example.GooRoomBe.account.application.port.in.dto.UserProfileDto;
import com.example.GooRoomBe.account.domain.repository.UserRepository;
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
    public List<UserProfileDto> getUserProfiles(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();

        return userRepository.findActivatedUsers(ids).stream()
                .map(UserProfileDto::from)
                .toList();
    }

    @Override
    public Optional<UserProfileDto> getActiveUserProfile(Long id) {
        return userRepository.findActivatedUser(id)
                .map(UserProfileDto::from);
    }
}