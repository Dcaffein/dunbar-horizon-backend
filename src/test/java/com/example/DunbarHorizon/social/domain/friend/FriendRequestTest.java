package com.example.DunbarHorizon.social.domain.friend;

import com.example.DunbarHorizon.social.domain.friend.exception.FriendRequestAuthorizationException;
import com.example.DunbarHorizon.social.domain.friend.exception.FriendRequestInvalidException;
import com.example.DunbarHorizon.social.domain.socialUser.SocialUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

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
    @DisplayName("요청자가 PENDING 상태의 요청에 대해 취소 권한을 검증하면 예외가 발생하지 않는다")
    void validateCancelBy_ByRequester_Success() {
        // given
        FriendRequest request = new FriendRequest(requester, receiver);

        // when & then
        assertThatNoException().isThrownBy(() -> request.validateCancelBy(1L));
    }

    @Test
    @DisplayName("수신자가 자신이 받은 요청의 취소 권한을 검증하면 예외가 발생한다")
    void validateCancelBy_ByReceiver_Fail() {
        // given
        FriendRequest request = new FriendRequest(requester, receiver);

        // when & then
        assertThatThrownBy(() -> request.validateCancelBy(2L))
                .isInstanceOf(FriendRequestAuthorizationException.class);
    }

    @Test
    @DisplayName("이미 수락된 요청에 대해 취소 권한을 검증하면 예외가 발생한다")
    void validateCancelBy_AfterAccepted_Fail() {
        // given
        FriendRequest request = new FriendRequest(requester, receiver);
        ReflectionTestUtils.setField(request, "status", FriendRequestStatus.ACCEPTED);

        // when & then
        assertThatThrownBy(() -> request.validateCancelBy(1L))
                .isInstanceOf(FriendRequestInvalidException.class);
    }

    @Test
    @DisplayName("HIDDEN 상태의 요청에 대해 취소 권한을 검증하면 예외가 발생한다")
    void validateCancelBy_WhenHidden_Fail() {
        // given
        FriendRequest request = new FriendRequest(requester, receiver);
        ReflectionTestUtils.setField(request, "status", FriendRequestStatus.HIDDEN);

        // when & then
        assertThatThrownBy(() -> request.validateCancelBy(1L))
                .isInstanceOf(FriendRequestInvalidException.class);
    }
}
