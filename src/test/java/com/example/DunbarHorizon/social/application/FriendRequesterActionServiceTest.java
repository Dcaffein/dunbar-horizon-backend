package com.example.DunbarHorizon.social.application;

import com.example.DunbarHorizon.social.application.service.FriendRequesterActionService;
import com.example.DunbarHorizon.social.domain.friend.FriendRequest;
import com.example.DunbarHorizon.social.domain.friend.FriendRequestStatus;
import com.example.DunbarHorizon.social.domain.friend.FriendTestFactory;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendRequestRepository;
import com.example.DunbarHorizon.social.domain.friend.FriendshipBroker;
import com.example.DunbarHorizon.social.domain.socialUser.SocialUser;
import com.example.DunbarHorizon.social.domain.socialUser.repository.SocialUserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FriendRequesterActionServiceTest {

    @InjectMocks
    private FriendRequesterActionService requesterService;

    @Mock
    private FriendRequestRepository friendRequestRepository;

    @Mock
    private SocialUserRepository socialUserRepository;

    @Mock
    private FriendshipBroker friendshipBroker;

    @Test
    @DisplayName("신청자가 친구 요청을 생성하면 유저 존재 확인 후 Broker를 통해 요청을 저장한다")
    void createFriendRequest_Success() {
        // given
        Long reqId = 1L;
        Long resId = 2L;
        SocialUser requester = mock(SocialUser.class);
        SocialUser receiver = mock(SocialUser.class);

        FriendRequest mockRequest = mock(FriendRequest.class);

        given(socialUserRepository.findById(reqId)).willReturn(Optional.of(requester));
        given(socialUserRepository.findById(resId)).willReturn(Optional.of(receiver));
        given(friendshipBroker.propose(requester, receiver)).willReturn(mockRequest);

        // when
        requesterService.sendRequest(reqId, resId);

        // then
        verify(friendRequestRepository).saveRequest(mockRequest);
    }

    @Test
    @DisplayName("신청자가 자신이 보낸 요청을 취소하면 상태가 DELETED로 변경되어 저장된다")
    void cancelFriendRequest_Success() {
        // given
        String requestId = "uuid-v7-id";
        Long requesterId = 1L;

        SocialUser req = mock(SocialUser.class);
        SocialUser res = mock(SocialUser.class);
        given(req.getId()).willReturn(requesterId);

        FriendRequest request = FriendTestFactory.createRequest(req, res);
        given(friendRequestRepository.findById(requestId)).willReturn(Optional.of(request));

        // when
        requesterService.cancelRequest(requestId, requesterId);

        // then
        assertThat(request.getStatus()).isEqualTo(FriendRequestStatus.DELETED);
        verify(friendRequestRepository).saveRequest(request);
    }
}