package com.example.DunbarHorizon.social.application.service;

import com.example.DunbarHorizon.social.domain.friend.repository.FriendRequestRepository;
import lombok.RequiredArgsConstructor;
import com.example.DunbarHorizon.global.annotation.Neo4jTransactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class FriendRequestCleanupService {

    private final FriendRequestRepository friendRequestRepository;

    @Neo4jTransactional
    public void deleteExpiredHiddenRequests() {
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
        friendRequestRepository.deleteOldHiddenRequests(oneMonthAgo);
    }
}
