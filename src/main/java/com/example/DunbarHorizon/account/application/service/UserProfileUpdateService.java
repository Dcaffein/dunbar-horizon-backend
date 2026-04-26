package com.example.DunbarHorizon.account.application.service;

import com.example.DunbarHorizon.account.application.port.in.UserProfileUpdateUseCase;
import com.example.DunbarHorizon.account.domain.exception.UserNotFoundException;
import com.example.DunbarHorizon.account.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserProfileUpdateService implements UserProfileUpdateUseCase {

    private final UserRepository userRepository;

    @Override
    public void updateProfile(Long userId, String nickname, String profileImageUrl) {
        userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("존재하지 않는 유저입니다."))
                .updateProfile(nickname, profileImageUrl);
    }
}
