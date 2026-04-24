package com.example.DunbarHorizon.social.application.service;

import com.example.DunbarHorizon.social.domain.friend.repository.FriendRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class FriendRequestCleanupService {

    private final FriendRequestRepository friendRequestRepository;

    @Transactional
    public void deleteExpiredHiddenRequests() {
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
        friendRequestRepository.deleteOldHiddenRequests(oneMonthAgo);
    }
}
