package com.example.GooRoomBe.social.friend.application;

import com.example.GooRoomBe.social.friend.domain.FriendRequest;
import com.example.GooRoomBe.social.friend.domain.FriendRequestStatus;
import com.example.GooRoomBe.social.friend.domain.factory.FriendRequestFactory;
import com.example.GooRoomBe.social.friend.exception.FriendRequestAuthorizationException;
import com.example.GooRoomBe.social.friend.exception.FriendRequestNotFoundException;
import com.example.GooRoomBe.social.friend.infrastructure.FriendRequestRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FriendRequestServiceTest {

    @Mock private FriendRequestRepository friendRequestRepository;
    @Mock private FriendRequestFactory friendRequestFactory;

    @InjectMocks
    private FriendRequestService friendRequestService;

    private final String REQUESTER_ID = "userA";
    private final String RECEIVER_ID = "userB";
    private final String REQUEST_ID = "req-1";

    @Test
    @DisplayName("createFriendRequest: 팩토리를 통해 요청을 생성하고 저장한다")
    void createFriendRequest_Success() {
        // Given
        FriendRequest mockRequest = mock(FriendRequest.class);
        given(friendRequestFactory.create(REQUESTER_ID, RECEIVER_ID)).willReturn(mockRequest);

        // When
        FriendRequest result = friendRequestService.createFriendRequest(REQUESTER_ID, RECEIVER_ID);

        // Then
        verify(friendRequestRepository).save(mockRequest);
        assertThat(result).isEqualTo(mockRequest);
    }

    @Test
    @DisplayName("updateFriendRequest: 요청 상태를 변경하고 저장한다")
    void updateFriendRequest_Success() {
        // Given
        FriendRequest mockRequest = mock(FriendRequest.class);
        given(friendRequestRepository.findById(REQUEST_ID)).willReturn(Optional.of(mockRequest));

        // When
        friendRequestService.updateFriendRequest(REQUEST_ID, RECEIVER_ID, FriendRequestStatus.ACCEPTED);

        // Then
        verify(mockRequest).updateStatus(FriendRequestStatus.ACCEPTED, RECEIVER_ID);
        verify(friendRequestRepository).save(mockRequest);
    }

    @Test
    @DisplayName("cancelFriendRequest: 검증 후 리포지토리에서 삭제한다")
    void cancelFriendRequest_Success() {
        // Given
        FriendRequest mockRequest = mock(FriendRequest.class);
        given(friendRequestRepository.findById(REQUEST_ID)).willReturn(Optional.of(mockRequest));

        // When
        friendRequestService.cancelFriendRequest(REQUEST_ID, REQUESTER_ID);

        // Then
        verify(mockRequest).checkCancelable(REQUESTER_ID);
        verify(friendRequestRepository).delete(mockRequest);
    }

    @Test
    @DisplayName("cancelFriendRequest: 검증 실패(예외 발생) 시 삭제하지 않는다")
    void cancelFriendRequest_Fail_Validation() {
        // Given
        FriendRequest mockRequest = mock(FriendRequest.class);
        given(friendRequestRepository.findById(REQUEST_ID)).willReturn(Optional.of(mockRequest));

        doThrow(new FriendRequestAuthorizationException("err", "err"))
                .when(mockRequest).checkCancelable(anyString());

        // When & Then
        assertThatThrownBy(() ->
                friendRequestService.cancelFriendRequest(REQUEST_ID, "stranger")
        ).isInstanceOf(FriendRequestAuthorizationException.class);
        verify(friendRequestRepository, never()).delete(any());
    }

    @Test
    @DisplayName("존재하지 않는 요청 ID로 조회 시 예외가 발생한다")
    void findFriendRequestById_NotFound() {
        // Given
        given(friendRequestRepository.findById("unknown")).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() ->
                friendRequestService.updateFriendRequest("unknown", RECEIVER_ID, FriendRequestStatus.ACCEPTED)
        ).isInstanceOf(FriendRequestNotFoundException.class);
    }
}