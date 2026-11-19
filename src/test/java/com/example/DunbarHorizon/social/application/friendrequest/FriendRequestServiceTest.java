package com.example.DunbarHorizon.social.application.friendrequest;

import com.example.DunbarHorizon.social.application.friendrequest.dto.FriendRequestDirection;
import com.example.DunbarHorizon.social.application.friendrequest.dto.FriendRequestResult;
import com.example.DunbarHorizon.social.application.friendrequest.dto.FriendRequestView;
import com.example.DunbarHorizon.social.application.friendrequest.dto.FriendResult;
import com.example.DunbarHorizon.social.application.friendrequest.port.FriendRequestQueryRepository;
import com.example.DunbarHorizon.social.domain.friendship.FriendTestFactory;
import com.example.DunbarHorizon.social.domain.friendship.FriendRequestStatus;
import com.example.DunbarHorizon.social.domain.friendship.FriendshipBroker;
import com.example.DunbarHorizon.social.domain.friendship.exception.FriendRequestInvalidException;
import com.example.DunbarHorizon.social.domain.friendship.repository.FriendRequestRepository;
import com.example.DunbarHorizon.social.domain.friendship.repository.FriendshipRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FriendRequestServiceTest {

    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 11, 17, 12, 0);

    @InjectMocks
    private FriendRequestService friendRequestService;

    @Mock
    private FriendRequestRepository friendRequestRepository;

    @Mock
    private FriendRequestQueryRepository friendRequestQueryRepository;

    @Mock
    private FriendshipRepository friendshipRepository;

    @Mock
    private FriendshipBroker friendshipBroker;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Test
    @DisplayName("발송 응답에 생성 시각을 유지하고 단건 조회에서 빠진 프로필은 null로 반환한다")
    void sendRequest_PreservesCreationTimeAndMissingProfile_FromCreatedRequest() {
        // given
        var request = FriendTestFactory.createRequest(2L, 1L);
        given(friendshipBroker.propose(2L, 1L, false, false)).willReturn(request);
        given(friendRequestQueryRepository.findSentTo(2L, 1L))
                .willReturn(Optional.of(new FriendRequestView(
                        2L, "requester", 1L, null, FriendRequestStatus.PENDING, CREATED_AT)));

        // when
        var result = friendRequestService.sendRequest(2L, 1L);

        // then
        verify(friendRequestRepository).create(request);
        assertThat(result).isEqualTo(new FriendRequestResult(
                "1_2", new FriendResult(2L, "requester"), null, FriendRequestStatus.PENDING, request.getCreatedAt()));
    }

    @Test
    @DisplayName("수신된 친구 요청 목록을 PENDING 상태로 조회한다")
    void getReceivedRequests_ReturnsPendingRequests() {
        // given
        Long userId = 2L;
        given(friendRequestQueryRepository.findReceived(userId, FriendRequestStatus.PENDING))
                .willReturn(List.of(view(FriendRequestStatus.PENDING)));

        // when
        List<FriendRequestResult> result = friendRequestService.getRequests(userId, FriendRequestDirection.RECEIVED, null);

        // then
        assertThat(result).singleElement().satisfies(request -> {
            assertThat(request.id()).isEqualTo("1_2");
            assertThat(request.requester().nickname()).isEqualTo("requester");
            assertThat(request.receiver().nickname()).isEqualTo("receiver");
            assertThat(request.status()).isEqualTo(FriendRequestStatus.PENDING);
        });
    }

    @Test
    @DisplayName("수신된 요청이 없으면 빈 목록을 반환한다")
    void getReceivedRequests_ReturnsEmpty_WhenNone() {
        // given
        given(friendRequestQueryRepository.findReceived(2L, FriendRequestStatus.PENDING)).willReturn(List.of());

        // when
        List<FriendRequestResult> result = friendRequestService.getRequests(2L, FriendRequestDirection.RECEIVED, null);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("숨김 처리된 친구 요청 목록을 HIDDEN 상태로 조회한다")
    void getHiddenRequests_ReturnsHiddenRequests() {
        // given
        given(friendRequestQueryRepository.findReceived(2L, FriendRequestStatus.HIDDEN))
                .willReturn(List.of(view(FriendRequestStatus.HIDDEN)));

        // when
        List<FriendRequestResult> result = friendRequestService.getRequests(2L, FriendRequestDirection.RECEIVED, FriendRequestStatus.HIDDEN);

        // then
        assertThat(result).singleElement().extracting(FriendRequestResult::status).isEqualTo(FriendRequestStatus.HIDDEN);
    }

    @Test
    @DisplayName("내가 보낸 친구 요청 목록을 PENDING 상태로 조회한다")
    void getSentRequests_ReturnsPendingRequests() {
        // given
        given(friendRequestQueryRepository.findSent(1L)).willReturn(List.of(view(FriendRequestStatus.PENDING)));

        // when
        List<FriendRequestResult> result = friendRequestService.getRequests(1L, FriendRequestDirection.SENT, null);

        // then
        assertThat(result).singleElement().extracting(FriendRequestResult::status).isEqualTo(FriendRequestStatus.PENDING);
    }

    @Test
    @DisplayName("보낸 요청 목록에서 수신자가 숨긴 요청은 PENDING으로 보인다")
    void getSentRequests_ShowsHiddenAsPending() {
        // given
        given(friendRequestQueryRepository.findSent(1L)).willReturn(List.of(view(FriendRequestStatus.HIDDEN)));

        // when
        List<FriendRequestResult> result = friendRequestService.getRequests(1L, FriendRequestDirection.SENT, null);

        // then
        assertThat(result).singleElement().extracting(FriendRequestResult::status).isEqualTo(FriendRequestStatus.PENDING);
    }

    @Test
    @DisplayName("sent 조회에 status를 사용하면 400 예외가 발생한다")
    void getRequests_SentWithStatus_ThrowsException() {
        assertThatThrownBy(() -> friendRequestService.getRequests(1L, FriendRequestDirection.SENT, FriendRequestStatus.HIDDEN))
                .isInstanceOf(FriendRequestInvalidException.class);
    }

    @Test
    @DisplayName("received 조회에 ACCEPTED를 사용하면 해당 상태로 조회한다")
    void getRequests_ReceivedAccepted_해당_상태로_조회한다() {
        // given
        given(friendRequestQueryRepository.findReceived(1L, FriendRequestStatus.ACCEPTED)).willReturn(List.of());

        // when
        List<FriendRequestResult> result = friendRequestService.getRequests(1L, FriendRequestDirection.RECEIVED, FriendRequestStatus.ACCEPTED);

        // then
        verify(friendRequestQueryRepository).findReceived(1L, FriendRequestStatus.ACCEPTED);
        assertThat(result).isEmpty();
    }

    private static FriendRequestView view(FriendRequestStatus status) {
        return new FriendRequestView(1L, "requester", 2L, "receiver", status, CREATED_AT);
    }
}
