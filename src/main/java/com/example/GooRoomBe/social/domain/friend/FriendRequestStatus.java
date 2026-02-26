package com.example.GooRoomBe.social.domain.friend;

import com.example.GooRoomBe.social.domain.friend.exception.FriendRequestAuthorizationException;
import com.example.GooRoomBe.social.domain.friend.exception.FriendRequestInvalidException;


public enum FriendRequestStatus {

    PENDING {
        @Override
        public FriendRequestStatus accept(FriendRequest request, Long userId) {
            validateReceiver(request, userId);
            return ACCEPTED;
        }

        @Override
        public FriendRequestStatus hide(FriendRequest request, Long userId) {
            validateReceiver(request, userId);
            return HIDDEN;
        }

        @Override
        public FriendRequestStatus cancel(FriendRequest request, Long userId) {
            validateRequester(request, userId);
            return DELETED;
        }
    },

    ACCEPTED {
        @Override
        public FriendRequestStatus complete(FriendRequest request) {
            return DELETED;
        }
    },

    HIDDEN {
        @Override
        public FriendRequestStatus accept(FriendRequest request, Long userId) {
            validateReceiver(request, userId);
            return ACCEPTED;
        }

        @Override
        public FriendRequestStatus undoHide(FriendRequest request, Long userId) {
            validateReceiver(request, userId);
            return PENDING;
        }
    },

    DELETED {};

    public FriendRequestStatus accept(FriendRequest request, Long userId) { return throwException("수락", this); }
    public FriendRequestStatus hide(FriendRequest request, Long userId) { return throwException("숨김", this); }
    public FriendRequestStatus undoHide(FriendRequest request, Long userId) { return throwException("숨김 해제", this); }
    public FriendRequestStatus cancel(FriendRequest request, Long userId) { return throwException("취소", this); }
    public FriendRequestStatus complete(FriendRequest request) { return throwException("완료 처리", this); }

    protected void validateReceiver(FriendRequest request, Long userId) {
        if (!request.getReceiver().getId().equals(userId)) {
            throw new FriendRequestAuthorizationException(request.getId(), userId);
        }
    }

    protected void validateRequester(FriendRequest request, Long userId) {
        if (!request.getRequester().getId().equals(userId)) {
            throw new FriendRequestAuthorizationException(request.getId(), userId);
        }
    }

    private FriendRequestStatus throwException(String action, FriendRequestStatus current) {
        throw new FriendRequestInvalidException(
                String.format("[%s] 상태에서는 [%s] 행위를 할 수 없습니다.", current.name(), action)
        );
    }
}