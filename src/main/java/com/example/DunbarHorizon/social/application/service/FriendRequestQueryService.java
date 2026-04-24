package com.example.DunbarHorizon.social.application.service;

import com.example.DunbarHorizon.social.application.port.in.FriendRequestQueryUseCase;
import com.example.DunbarHorizon.social.application.dto.result.FriendRequestResult;
import com.example.DunbarHorizon.social.domain.friend.FriendRequestStatus;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FriendRequestQueryService implements FriendRequestQueryUseCase {
    private final FriendRequestRepository friendRequestRepository;

    @Override
    public List<FriendRequestResult> getReceivedRequests(Long userId) {
        return friendRequestRepository.findAllByReceiver_IdAndStatus(userId, FriendRequestStatus.PENDING)
                .stream()
                .map(FriendRequestResult::from)
                .toList();
    }

    @Override
    public List<FriendRequestResult> getHiddenRequests(Long userId) {
        return friendRequestRepository.findAllByReceiver_IdAndStatus(userId, FriendRequestStatus.HIDDEN)
                .stream()
                .map(FriendRequestResult::from)
                .toList();
    }

    @Override
    public List<FriendRequestResult> getSentRequests(Long userId) {
        return friendRequestRepository.findAllByRequester_IdAndStatus(userId, FriendRequestStatus.PENDING)
                .stream()
                .map(FriendRequestResult::from)
                .toList();
    }
}
