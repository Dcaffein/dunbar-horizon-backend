package com.example.GooRoomBe.social.domain.friend;

import com.example.GooRoomBe.social.domain.friend.exception.FriendRequestAuthorizationException;
import com.example.GooRoomBe.social.domain.friend.exception.FriendRequestInvalidException;
import com.example.GooRoomBe.social.domain.socialUser.SocialUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

class FriendRequestTest {

    private SocialUser requester;
    private SocialUser receiver;

    @BeforeEach
    void setUp() {
        requester = new SocialUser(1L, "요청자", "");
        receiver = new SocialUser(2L, "수신자", "");
    }

    @Test
    @DisplayName("수신자가 친구 요청을 수락하면 상태가 ACCEPTED가 된다")
    void accept_Success() {
        // given
        FriendRequest friendRequest = new FriendRequest(requester, receiver);

        // when
        friendRequest.accept(2L);

        // then
        assertThat(friendRequest.getStatus()).isEqualTo(FriendRequestStatus.ACCEPTED);
    }

    @Test
    @DisplayName("요청자가 수락을 시도하면 예외가 발생한다")
    void accept_ByRequester_Fail() {
        // given
        FriendRequest request = new FriendRequest(requester, receiver);

        // when & then
        assertThatThrownBy(() -> request.accept(1L))
                .isInstanceOf(FriendRequestAuthorizationException.class);
    }

    @Test
    @DisplayName("이미 수락된 요청은 취소할 수 없다")
    void cancel_AfterAccepted_Fail() {
        // given
        FriendRequest request = new FriendRequest(requester, receiver);
        ReflectionTestUtils.setField(request, "status", FriendRequestStatus.ACCEPTED);

        // when & then
        assertThatThrownBy(() -> request.cancel(1L))
                .isInstanceOf(FriendRequestInvalidException.class);
    }

    @Test
    @DisplayName("요청자가 PENDING 상태의 요청을 취소하면 상태가 DELETED가 된다")
    void cancel_ByRequester_Success() {
        // given
        FriendRequest request = new FriendRequest(requester, receiver);

        // when
        request.cancel(1L);

        // then
        assertThat(request.getStatus()).isEqualTo(FriendRequestStatus.DELETED);
    }
}
