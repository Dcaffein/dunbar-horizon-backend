package com.example.DunbarHorizon.social.domain.friend;

import com.example.DunbarHorizon.social.domain.friend.exception.CannotRequestToSelfException;
import com.example.DunbarHorizon.social.domain.socialUser.UserReference;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.*;

import java.time.LocalDateTime;

import static com.example.DunbarHorizon.social.domain.friend.constant.FriendConstants.SENT_FRIEND_REQUEST;
import static com.example.DunbarHorizon.social.domain.friend.constant.FriendConstants.FRIEND_REQUEST_TO;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Node("FriendRequest")
public class FriendRequest {
    @Id
    private String id;

    @Relationship(type = SENT_FRIEND_REQUEST, direction = Relationship.Direction.INCOMING)
    private UserReference requester;

    @Relationship(type = FRIEND_REQUEST_TO, direction = Relationship.Direction.OUTGOING)
    private UserReference receiver;

    private LocalDateTime createdAt;

    private FriendRequestStatus status;

    FriendRequest(UserReference requester, UserReference receiver) {
        if (requester.getId().equals(receiver.getId())) {
            throw new CannotRequestToSelfException(requester.getId());
        }
        this.id = generateId(requester.getId(), receiver.getId());
        this.requester = requester;
        this.receiver = receiver;
        this.status = FriendRequestStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public static String generateId(Long id1, Long id2) {
        long min = Math.min(id1, id2);
        long max = Math.max(id1, id2);
        return min + "_" + max;
    }

    public void accept(Long userId) {
        this.status = this.status.accept(this, userId);
    }

    public void hide(Long userId) {
        this.status = this.status.hide(this, userId);
    }

    public void undoHide(Long userId) {
        this.status = this.status.undoHide(this, userId);
    }

    public void validateCancelBy(Long userId) {
        this.status.validateCancelBy(this, userId);
    }

    public boolean isAccepted() {
        return this.status == FriendRequestStatus.ACCEPTED;
    }
}