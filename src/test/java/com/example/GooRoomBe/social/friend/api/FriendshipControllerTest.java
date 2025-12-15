package com.example.GooRoomBe.social.friend.api;

import com.example.GooRoomBe.social.friend.api.dto.FriendUpdateRequestDto;
import com.example.GooRoomBe.social.friend.application.FriendshipService;
import com.example.GooRoomBe.support.ControllerTestSupport;
import com.example.GooRoomBe.support.WithCustomMockUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FriendshipController.class)
class FriendshipControllerTest extends ControllerTestSupport {

    @MockitoBean
    private FriendshipService friendshipService;

    @Test
    @DisplayName("친구 정보 수정(별명 등): 성공 시 200 OK를 반환한다")
    @WithCustomMockUser(userId = "my-id")
    void updateFriend_Success() throws Exception {
        // Given
        String friendId = "friend-id";
        FriendUpdateRequestDto updateDto = new FriendUpdateRequestDto("Bestie", true);

        // When & Then
        mockMvc.perform(patch("/api/v1/friends/{friendId}", friendId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andDo(print())
                .andExpect(status().isOk());

        // Verify
        verify(friendshipService).updateFriendProps(
                eq("my-id"),
                eq(friendId),
                refEq(updateDto)
        );
    }

    @Test
    @DisplayName("친구 삭제(관계 끊기): 성공 시 204 No Content를 반환한다")
    @WithCustomMockUser(userId = "my-id")
    void deleteFriendship_Success() throws Exception {
        // Given
        String friendId = "friend-id";

        // When & Then
        mockMvc.perform(delete("/api/v1/friends/{friendId}", friendId)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isNoContent());

        // Verify
        verify(friendshipService).deleteFriendShip("my-id", friendId);
    }
}