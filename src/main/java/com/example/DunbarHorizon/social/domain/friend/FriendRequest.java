package com.example.DunbarHorizon.social.domain.friend;

import com.example.DunbarHorizon.global.exception.ErrorContext;
import com.example.DunbarHorizon.social.domain.exception.SocialErrorCode;
import com.example.DunbarHorizon.social.domain.friend.exception.CannotRequestToSelfException;
import com.example.DunbarHorizon.social.domain.friend.exception.FriendRequestInvalidException;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "friend_requests")
@IdClass(FriendRequestId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FriendRequest {

    @Id
    private Long requesterId;

    @Id
    private Long receiverId;

    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    private FriendRequestStatus status;

    FriendRequest(Long requesterId, Long receiverId) {
        if (requesterId.equals(receiverId)) {
            throw new CannotRequestToSelfException(requesterId);
        }
        this.requesterId = requesterId;
        this.receiverId = receiverId;
        this.status = FriendRequestStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public void updateStatus(Long userId, FriendRequestStatus targetStatus) {
        if (targetStatus == null) {
            throw new FriendRequestInvalidException(SocialErrorCode.FRIEND_REQUEST_STATUS_REQUIRED,
                    ErrorContext.of("requesterId", requesterId).and("receiverId", receiverId));
        }

        this.status = targetStatus.update(this, userId);
    }

    public void cancel(Long userId) {
        this.status.cancel(this, userId);
    }

    public FriendRequestStatus getStatusFor(Long viewerId) {
        if (status == FriendRequestStatus.HIDDEN && requesterId.equals(viewerId)) {
            return FriendRequestStatus.PENDING;
        }
        return status;
    }

    public boolean isAccepted() {
        return this.status == FriendRequestStatus.ACCEPTED;
    }
}
