package com.example.GooRoomBe.social.application.service;

import com.example.GooRoomBe.social.application.port.in.FriendRequestQueryUseCase;
import com.example.GooRoomBe.social.application.port.in.dto.FriendRequestResponse;
import com.example.GooRoomBe.social.domain.friend.FriendRequestStatus;
import com.example.GooRoomBe.social.domain.friend.repository.FriendRequestRepository;
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
    public List<FriendRequestResponse> getReceivedRequests(Long userId) {
        return friendRequestRepository.findAllByReceiver_IdAndStatus(userId, FriendRequestStatus.PENDING)
                .stream()
                .map(FriendRequestResponse::from)
                .toList();
    }

}
