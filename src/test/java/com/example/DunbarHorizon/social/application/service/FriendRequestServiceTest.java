package com.example.DunbarHorizon.social.application.service;

import com.example.DunbarHorizon.social.application.dto.result.FriendRequestResult;
import com.example.DunbarHorizon.social.application.dto.FriendRequestDirection;
import com.example.DunbarHorizon.social.domain.friend.FriendRequest;
import com.example.DunbarHorizon.social.domain.friend.FriendRequestStatus;
import com.example.DunbarHorizon.social.domain.friend.FriendTestFactory;
import com.example.DunbarHorizon.social.domain.friend.exception.FriendRequestInvalidException;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendRequestRepository;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendshipRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Map;
import com.example.DunbarHorizon.social.application.dto.SocialUserProfile;
import com.example.DunbarHorizon.social.application.port.out.UserProfileRepository;

import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FriendRequestServiceTest {

    @InjectMocks
    private FriendRequestService service;

    @Mock
    private FriendRequestRepository friendRequestRepository;

    @Mock
    private FriendshipRepository friendshipRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Test
    @DisplayName("수신된 친구 요청 목록을 PENDING 상태로 조회한다")
    void getReceivedRequests_ReturnsPendingRequests() {
        // given
        Long userId = 1L;
        FriendRequest request = FriendTestFactory.createRequest(1L, 2L);

        given(friendRequestRepository.findAllByReceiverIdAndStatus(userId, FriendRequestStatus.PENDING))
                .willReturn(List.of(request));

        given(userProfileRepository.findActiveProfiles(List.of(1L, 2L)))
                .willReturn(Map.of(2L, new SocialUserProfile(2L, "receiver", null)));

        // when
        List<FriendRequestResult> result = service.getRequests(
                userId, FriendRequestDirection.RECEIVED, null);

        // then
        assertThat(result.getFirst().receiver().nickname()).isEqualTo("receiver");
    }

    @Test
    @DisplayName("수신된 요청이 없으면 빈 목록을 반환한다")
    void getReceivedRequests_ReturnsEmpty_WhenNone() {
        // given
        Long userId = 1L;
        given(friendRequestRepository.findAllByReceiverIdAndStatus(userId, FriendRequestStatus.PENDING))
                .willReturn(List.of());

        // when
        List<FriendRequestResult> result = service.getRequests(
                userId, FriendRequestDirection.RECEIVED, null);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("숨김 처리된 친구 요청 목록을 HIDDEN 상태로 조회한다")
    void getHiddenRequests_ReturnsHiddenRequests() {
        // given
        Long userId = 2L;
        FriendRequest request = FriendTestFactory.createRequest(1L, 2L);
        request.updateStatus(userId, FriendRequestStatus.HIDDEN);

        given(friendRequestRepository.findAllByReceiverIdAndStatus(userId, FriendRequestStatus.HIDDEN))
                .willReturn(List.of(request));

        // when
        List<FriendRequestResult> result = service.getRequests(
                userId, FriendRequestDirection.RECEIVED, FriendRequestStatus.HIDDEN);

        // then
        assertThat(result).singleElement().extracting(FriendRequestResult::status)
                .isEqualTo(FriendRequestStatus.HIDDEN);
    }

    @Test
    @DisplayName("내가 보낸 친구 요청 목록을 PENDING 상태로 조회한다")
    void getSentRequests_ReturnsPendingRequests() {
        // given
        Long userId = 1L;
        FriendRequest request = FriendTestFactory.createRequest(1L, 2L);

        given(friendRequestRepository.findAllByRequesterId(userId)).willReturn(List.of(request));

        // when
        List<FriendRequestResult> result = service.getRequests(
                userId, FriendRequestDirection.SENT, null);

        // then
        assertThat(result).singleElement().extracting(FriendRequestResult::status)
                .isEqualTo(FriendRequestStatus.PENDING);
    }

    @Test
    @DisplayName("보낸 요청 목록에서 수신자가 숨긴 요청은 PENDING으로 보인다")
    void getSentRequests_ShowsHiddenAsPending() {
        // given
        Long userId = 1L;
        FriendRequest request = FriendTestFactory.createRequest(1L, 2L);
        request.updateStatus(2L, FriendRequestStatus.HIDDEN);

        given(friendRequestRepository.findAllByRequesterId(userId)).willReturn(List.of(request));

        // when
        List<FriendRequestResult> result = service.getRequests(
                userId, FriendRequestDirection.SENT, null);

        // then
        assertThat(result).singleElement().extracting(FriendRequestResult::status)
                .isEqualTo(FriendRequestStatus.PENDING);
    }

    @Test
    @DisplayName("sent 조회에 status를 사용하면 400 예외가 발생한다")
    void getRequests_SentWithStatus_ThrowsException() {
        assertThatThrownBy(() -> service.getRequests(
                1L, FriendRequestDirection.SENT, FriendRequestStatus.HIDDEN))
                .isInstanceOf(FriendRequestInvalidException.class);
    }

    @Test
    @DisplayName("received 조회에 ACCEPTED를 사용하면 해당 상태로 조회한다")
    void getRequests_ReceivedAccepted_해당_상태로_조회한다() {
        // given
        given(friendRequestRepository.findAllByReceiverIdAndStatus(1L, FriendRequestStatus.ACCEPTED))
                .willReturn(List.of());

        // when
        List<FriendRequestResult> result = service.getRequests(
                1L, FriendRequestDirection.RECEIVED, FriendRequestStatus.ACCEPTED);

        // then
        verify(friendRequestRepository).findAllByReceiverIdAndStatus(1L, FriendRequestStatus.ACCEPTED);
        assertThat(result).isEmpty();
    }
}
