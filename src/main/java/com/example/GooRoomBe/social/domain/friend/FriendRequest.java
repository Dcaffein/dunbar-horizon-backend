package com.example.GooRoomBe.social.domain.friend;

import com.example.GooRoomBe.global.util.UuidUtil;
import com.example.GooRoomBe.social.domain.socialUser.UserReference;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.*;

import java.time.LocalDateTime;

import static com.example.GooRoomBe.social.domain.friend.constant.FriendConstants.SENT;
import static com.example.GooRoomBe.social.domain.friend.constant.FriendConstants.TO;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Node("FriendRequest")
public class FriendRequest {
    @Id
    private String id;

    @Relationship(type = SENT, direction = Relationship.Direction.INCOMING)
    private UserReference requester;

    @Relationship(type = TO, direction = Relationship.Direction.OUTGOING)
    private UserReference receiver;

    private LocalDateTime createdAt;

    private FriendRequestStatus status;

    FriendRequest(UserReference requester, UserReference receiver) {
        this.id = UuidUtil.createV7().toString();
        this.requester = requester;
        this.receiver = receiver;
        this.status = FriendRequestStatus.PENDING;
        this.createdAt = LocalDateTime.now();
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

    public void cancel(Long userId) {
        this.status = this.status.cancel(this, userId);
    }

    public void complete() {
        this.status = this.status.complete(this);
    }

    public boolean isAccepted() {
        return this.status == FriendRequestStatus.ACCEPTED;
    }
}