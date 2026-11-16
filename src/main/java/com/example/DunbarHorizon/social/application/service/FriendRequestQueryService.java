package com.example.DunbarHorizon.social.application.service;



import com.example.DunbarHorizon.global.exception.ErrorContext;import com.example.DunbarHorizon.social.domain.exception.SocialErrorCode;import com.example.DunbarHorizon.social.application.port.in.FriendRequestQueryUseCase;
import com.example.DunbarHorizon.social.application.dto.result.FriendRequestResult;
import com.example.DunbarHorizon.social.application.port.out.SocialUserProfileReader;
import com.example.DunbarHorizon.social.domain.friend.FriendRequest;
import com.example.DunbarHorizon.social.application.dto.FriendRequestDirection;
import com.example.DunbarHorizon.social.domain.friend.exception.FriendRequestInvalidException;
import com.example.DunbarHorizon.social.domain.friend.FriendRequestStatus;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FriendRequestQueryService implements FriendRequestQueryUseCase {
    private final FriendRequestRepository friendRequestRepository;
    private final SocialUserProfileReader profiles;

    @Override
    public List<FriendRequestResult> getRequests(
            Long userId, FriendRequestDirection direction, FriendRequestStatus status) {
        FriendRequestStatus queryStatus = status == null ? FriendRequestStatus.PENDING : status;

        if (direction == FriendRequestDirection.SENT) {
            if (status != null) {
                throw new FriendRequestInvalidException(SocialErrorCode.FRIEND_REQUEST_STATUS_NOT_ALLOWED_FOR_SENT,
                        ErrorContext.of("status", status));
            }
            return toResults(userId, friendRequestRepository.findAllByRequesterId(userId));
        }

        return toResults(userId, friendRequestRepository.findAllByReceiverIdAndStatus(userId, queryStatus));
    }

    private List<FriendRequestResult> toResults(Long viewerId, List<FriendRequest> requests) {
        var ids = requests.stream().flatMap(request -> Stream.of(request.getRequesterId(), request.getReceiverId()))
                .filter(Objects::nonNull).distinct().toList();
        var users = profiles.findActiveProfiles(ids);
        return requests.stream()
                .map(request -> FriendRequestResult.from(request, viewerId,
                        request.getRequesterId() == null ? null : users.get(request.getRequesterId()),
                        request.getReceiverId() == null ? null : users.get(request.getReceiverId())))
                .toList();
    }
}
