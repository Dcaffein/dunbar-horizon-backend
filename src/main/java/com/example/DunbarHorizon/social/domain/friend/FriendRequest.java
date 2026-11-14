package com.example.DunbarHorizon.social.domain.friend;



import com.example.DunbarHorizon.global.exception.ErrorContext;import com.example.DunbarHorizon.social.domain.exception.SocialErrorCode;import com.example.DunbarHorizon.social.domain.friend.exception.CannotRequestToSelfException;
import com.example.DunbarHorizon.social.domain.friend.exception.FriendRequestInvalidException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class FriendRequest {
    private String id;

    private Long requesterId;

    private Long receiverId;

    private LocalDateTime createdAt;

    private FriendRequestStatus status;

    FriendRequest(Long requesterId, Long receiverId) {
        if (requesterId.equals(receiverId)) {
            throw new CannotRequestToSelfException(requesterId);
        }
        this.id = generateId(requesterId, receiverId);
        this.requesterId = requesterId;
        this.receiverId = receiverId;
        this.status = FriendRequestStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public static String generateId(Long id1, Long id2) {
        long min = Math.min(id1, id2);
        long max = Math.max(id1, id2);
        return min + "_" + max;
    }

    public void updateStatus(Long userId, FriendRequestStatus targetStatus) {
        if (targetStatus == null) {
            throw new FriendRequestInvalidException(SocialErrorCode.FRIEND_REQUEST_STATUS_REQUIRED,
                    ErrorContext.of("requestId", id));
        }

        this.status = targetStatus.update(this, userId);
    }

    public void cancel(Long userId) {
        this.status.cancel(this, userId);
    }

    public boolean isAccepted() {
        return this.status == FriendRequestStatus.ACCEPTED;
    }
}
