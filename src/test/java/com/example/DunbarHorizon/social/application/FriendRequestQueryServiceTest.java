package com.example.DunbarHorizon.social.application;

import com.example.DunbarHorizon.social.application.dto.result.FriendRequestResult;
import com.example.DunbarHorizon.social.application.service.FriendRequestQueryService;
import com.example.DunbarHorizon.social.domain.friend.FriendRequest;
import com.example.DunbarHorizon.social.domain.friend.FriendRequestStatus;
import com.example.DunbarHorizon.social.domain.friend.FriendTestFactory;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendRequestRepository;
import com.example.DunbarHorizon.social.domain.socialUser.SocialUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class FriendRequestQueryServiceTest {

    @InjectMocks
    private FriendRequestQueryService queryService;

    @Mock
    private FriendRequestRepository friendRequestRepository;

    @Test
    @DisplayName("수신된 친구 요청 목록을 PENDING 상태로 조회한다")
    void getReceivedRequests_ReturnsPendingRequests() {
        // given
        Long userId = 1L;
        SocialUser req = mock(SocialUser.class);
        SocialUser res = mock(SocialUser.class);
        FriendRequest request = FriendTestFactory.createRequest(req, res);

        given(friendRequestRepository.findAllByReceiver_IdAndStatus(userId, FriendRequestStatus.PENDING))
                .willReturn(List.of(request));

        // when
        List<FriendRequestResult> result = queryService.getReceivedRequests(userId);

        // then
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("수신된 요청이 없으면 빈 목록을 반환한다")
    void getReceivedRequests_ReturnsEmpty_WhenNone() {
        // given
        Long userId = 1L;
        given(friendRequestRepository.findAllByReceiver_IdAndStatus(userId, FriendRequestStatus.PENDING))
                .willReturn(List.of());

        // when
        List<FriendRequestResult> result = queryService.getReceivedRequests(userId);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("숨김 처리된 친구 요청 목록을 HIDDEN 상태로 조회한다")
    void getHiddenRequests_ReturnsHiddenRequests() {
        // given
        Long userId = 2L;
        SocialUser req = mock(SocialUser.class);
        SocialUser res = mock(SocialUser.class);
        given(res.getId()).willReturn(userId);
        FriendRequest request = FriendTestFactory.createRequest(req, res);
        request.hide(userId);

        given(friendRequestRepository.findAllByReceiver_IdAndStatus(userId, FriendRequestStatus.HIDDEN))
                .willReturn(List.of(request));

        // when
        List<FriendRequestResult> result = queryService.getHiddenRequests(userId);

        // then
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("내가 보낸 친구 요청 목록을 PENDING 상태로 조회한다")
    void getSentRequests_ReturnsPendingRequests() {
        // given
        Long userId = 1L;
        SocialUser req = mock(SocialUser.class);
        SocialUser res = mock(SocialUser.class);
        FriendRequest request = FriendTestFactory.createRequest(req, res);

        given(friendRequestRepository.findAllByRequester_IdAndStatus(userId, FriendRequestStatus.PENDING))
                .willReturn(List.of(request));

        // when
        List<FriendRequestResult> result = queryService.getSentRequests(userId);

        // then
        assertThat(result).hasSize(1);
    }
}
