package com.example.DunbarHorizon.social.adapter.in.web;

import com.example.DunbarHorizon.social.adapter.in.web.dto.FriendUpdateRequest;
import com.example.DunbarHorizon.social.application.port.in.command.FriendshipUpdateCommand;
import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.support.BaseControllerTest;
import com.example.DunbarHorizon.support.WithMockCustomUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WithMockCustomUser
class FriendshipControllerTest extends BaseControllerTest {

    @Test
    @DisplayName("친구 정보를 수정(별명, 소개글 노출 여부 등) 한다")
    void updateFriend_Success() throws Exception {
        Long friendId = 2L;
        FriendUpdateRequest dto = new FriendUpdateRequest("동기", false, true);

        mockMvc.perform(patch("/api/v1/friends/{friendId}", friendId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNoContent());

        verify(friendshipCommandUseCase).updateFriendship(anyLong(), eq(friendId), any(FriendshipUpdateCommand.class));
    }

    @Test
    @DisplayName("별명을 빈 문자열로 보내면 validation을 통과하고 alias가 삭제된다")
    void updateFriend_withEmptyAlias_returnsNoContent() throws Exception {
        Long friendId = 2L;
        FriendUpdateRequest dto = new FriendUpdateRequest("", null, null);

        mockMvc.perform(patch("/api/v1/friends/{friendId}", friendId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNoContent());

        verify(friendshipCommandUseCase).updateFriendship(anyLong(), eq(friendId), any(FriendshipUpdateCommand.class));
    }

    @Test
    @DisplayName("별명이 20자를 초과하면 400과 validation.friendAlias를 반환한다")
    void updateFriend_Fail_TooLongAlias() throws Exception {
        Long friendId = 2L;
        String tooLong = "가".repeat(Friendship.ALIAS_MAX_LENGTH + 1);
        FriendUpdateRequest dto = new FriendUpdateRequest(tooLong, null, null);

        mockMvc.perform(patch("/api/v1/friends/{friendId}", friendId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validation.friendAlias").value(Friendship.ALIAS_LENGTH_MESSAGE));
    }

    @Test
    @DisplayName("친구 관계를 성공적으로 해제한다")
    void brokeUpWithFriend_Success() throws Exception {
        Long friendId = 2L;

        mockMvc.perform(delete("/api/v1/friends/{friendId}", friendId))
                .andExpect(status().isNoContent());

        verify(friendshipCommandUseCase).brokeUpWith(anyLong(), eq(friendId));
    }
}
