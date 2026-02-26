package com.example.GooRoomBe.account.application.service;

import com.example.GooRoomBe.account.application.port.in.AccountCleanupUseCase;
import com.example.GooRoomBe.account.domain.repository.AuthRepository;
import com.example.GooRoomBe.account.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountCleanupService implements AccountCleanupUseCase {

    private final AuthRepository authRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void cleanupExpiredPendingAccounts() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(1);

        int deletedAuths = authRepository.deleteOldUnverifiedAuths(threshold);
        int deletedUsers = userRepository.deleteOldPendingUsers(threshold);

        log.info("Account Cleanup Completed: Deleted {} unverified Auths, {} pending Users",
                deletedAuths, deletedUsers);
    }
}
