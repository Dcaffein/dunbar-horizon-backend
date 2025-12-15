package com.example.GooRoomBe.social.friend.api;

import com.example.GooRoomBe.social.friend.api.dto.FriendRequestCreateDto;
import com.example.GooRoomBe.social.friend.api.dto.FriendRequestUpdateDto;
import com.example.GooRoomBe.social.friend.application.FriendRequestService;
import com.example.GooRoomBe.social.friend.domain.FriendRequest;
import com.example.GooRoomBe.social.friend.domain.FriendRequestStatus;
import com.example.GooRoomBe.social.socialUser.SocialUser;
import com.example.GooRoomBe.support.ControllerTestSupport;
import com.example.GooRoomBe.support.WithCustomMockUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FriendRequestController.class)
class FriendRequestControllerTest extends ControllerTestSupport {

    @MockitoBean
    private FriendRequestService friendRequestService;

    @Test
    @DisplayName("친구 요청 보내기: 성공 시 201 Created와 Location 헤더를 반환한다")
    @WithCustomMockUser(userId = "requester-id")
    void sendFriendRequest_Success() throws Exception {
        // Given
        String receiverId = "receiver-id";
        FriendRequestCreateDto requestDto = new FriendRequestCreateDto(receiverId);

        FriendRequest mockFriendRequest = mock(FriendRequest.class);
        given(mockFriendRequest.getId()).willReturn("req-uuid-123");

        SocialUser mockRequester = mock(SocialUser.class);
        SocialUser mockReceiver = mock(SocialUser.class);

        given(mockRequester.getId()).willReturn("requester-id");
        given(mockRequester.getNickname()).willReturn("RequesterNick");

        given(mockReceiver.getId()).willReturn(receiverId);
        given(mockReceiver.getNickname()).willReturn("ReceiverNick");

        given(mockFriendRequest.getRequester()).willReturn(mockRequester);
        given(mockFriendRequest.getReceiver()).willReturn(mockReceiver);

        given(friendRequestService.createFriendRequest("requester-id", receiverId))
                .willReturn(mockFriendRequest);

        // When & Then
        mockMvc.perform(post("/api/v1/friend-requests")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/friend-requests/req-uuid-123"));
    }

    @Test
    @DisplayName("친구 요청 상태 변경: 성공 시 200 OK를 반환한다")
    @WithCustomMockUser(userId = "user-id")
    void updateFriendRequest_Success() throws Exception {
        // Given
        String requestId = "req-123";
        FriendRequestUpdateDto updateDto = new FriendRequestUpdateDto(FriendRequestStatus.ACCEPTED);

        // When & Then
        mockMvc.perform(patch("/api/v1/friend-requests/{requestId}", requestId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andDo(print())
                .andExpect(status().isOk());

        // Verify Service Call
        verify(friendRequestService).updateFriendRequest(
                eq(requestId),
                eq("user-id"),
                eq(FriendRequestStatus.ACCEPTED)
        );
    }

    @Test
    @DisplayName("친구 요청 취소/거절: 성공 시 204 No Content를 반환한다")
    @WithCustomMockUser(userId = "user-id")
    void deleteFriendRequest_Success() throws Exception {
        // Given
        String requestId = "req-123";

        // When & Then
        mockMvc.perform(delete("/api/v1/friend-requests/{requestId}", requestId)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isNoContent());

        // Verify Service Call
        verify(friendRequestService).cancelFriendRequest(requestId, "user-id");
    }
}