package com.example.DunbarHorizon.social.adapter.in.web;

import com.example.DunbarHorizon.social.adapter.in.web.dto.FriendRequestCreateRequest;
import com.example.DunbarHorizon.social.application.port.in.FriendRequestReceiverActionUseCase;
import com.example.DunbarHorizon.social.application.port.in.FriendRequestQueryUseCase;
import com.example.DunbarHorizon.social.application.port.in.FriendRequesterActionUseCase;
import com.example.DunbarHorizon.social.domain.friend.FriendRequest;
import com.example.DunbarHorizon.social.domain.friend.FriendRequestStatus;
import com.example.DunbarHorizon.social.domain.socialUser.SocialUser;
import com.example.DunbarHorizon.support.BaseControllerTest;
import com.example.DunbarHorizon.support.WithMockCustomUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FriendRequestController.class)
@WithMockCustomUser()
class FriendRequestControllerTest extends BaseControllerTest {

    @MockitoBean
    private FriendRequesterActionUseCase requesterActionUseCase;

    @MockitoBean
    private FriendRequestReceiverActionUseCase receiverActionUseCase;

    @MockitoBean
    private FriendRequestQueryUseCase queryUseCase;

    @Test
    @DisplayName("친구 요청을 성공적으로 보낸다")
    void sendFriendRequest_Success() throws Exception {
        // given
        Long receiverId = 2L;
        FriendRequestCreateRequest dto = new FriendRequestCreateRequest(receiverId);

        FriendRequest mockRequest = mock(FriendRequest.class);
        SocialUser mockRequester = mock(SocialUser.class);
        SocialUser mockReceiver = mock(SocialUser.class);

        given(mockRequest.getId()).willReturn("newRequest");
        given(mockRequest.getRequester()).willReturn(mockRequester);
        given(mockRequest.getReceiver()).willReturn(mockReceiver);
        given(mockRequest.getStatus()).willReturn(FriendRequestStatus.PENDING);

        given(mockRequester.getId()).willReturn(1L);
        given(mockReceiver.getId()).willReturn(2L);

        given(requesterActionUseCase.sendRequest(eq(1L), eq(receiverId)))
                .willReturn(mockRequest);

        // when & then
        mockMvc.perform(post("/api/v1/friend-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").value("newRequest"));
    }

    @Test
    @DisplayName("친구 요청을 수락한다")
    void acceptFriendRequest_Success() throws Exception {
        // given
        String requestId = "newRequest";

        // when & then
        mockMvc.perform(post("/api/v1/friend-requests/{requestId}/accept", requestId))
                .andExpect(status().isNoContent());

        verify(receiverActionUseCase).acceptRequest(eq(requestId), eq(1L));
    }

    @Test
    @DisplayName("친구 요청을 숨긴다")
    void hideFriendRequest_Success() throws Exception {
        // given
        String requestId = "newRequest";

        // when & then
        mockMvc.perform(post("/api/v1/friend-requests/{requestId}/hide", requestId))
                .andExpect(status().isNoContent());

        verify(receiverActionUseCase).hideRequest(eq(requestId), eq(1L));
    }

    @Test
    @DisplayName("친구 요청 숨김을 취소한다")
    void undoHideFriendRequest_Success() throws Exception {
        // given
        String requestId = "newRequest";

        // when & then
        mockMvc.perform(delete("/api/v1/friend-requests/{requestId}/hide", requestId))
                .andExpect(status().isNoContent());

        verify(receiverActionUseCase).undoHideRequest(eq(requestId), eq(1L));
    }

    @Test
    @DisplayName("친구 요청을 취소한다")
    void cancelFriendRequest_Success() throws Exception {
        // given
        String requestId = "newRequest";

        // when & then
        mockMvc.perform(delete("/api/v1/friend-requests/{requestId}", requestId))
                .andExpect(status().isNoContent());

        verify(requesterActionUseCase).cancelRequest(eq(requestId), eq(1L));
    }
}
