package com.example.DunbarHorizon.social.domain.friend;

import com.example.DunbarHorizon.social.domain.friend.exception.FriendRequestAuthorizationException;
import com.example.DunbarHorizon.social.domain.friend.exception.FriendRequestInvalidException;


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
        public void validateCancelBy(FriendRequest request, Long userId) {
            validateRequester(request, userId);
        }
    },

    ACCEPTED,

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
    };

    public FriendRequestStatus accept(FriendRequest request, Long userId) { return throwInvalidException("수락"); }
    public FriendRequestStatus hide(FriendRequest request, Long userId) { return throwInvalidException("숨김"); }
    public FriendRequestStatus undoHide(FriendRequest request, Long userId) { return throwInvalidException("숨김 해제"); }
    public void validateCancelBy(FriendRequest request, Long userId) { throwInvalidException("취소"); }

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

    private FriendRequestStatus throwInvalidException(String action) {
        throw new FriendRequestInvalidException(
                String.format("[%s] 상태에서는 [%s] 행위를 할 수 없습니다.", this.name(), action)
        );
    }
}
