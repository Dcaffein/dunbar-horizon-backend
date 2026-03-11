package com.example.DunbarHorizon.social.adapter.in.web;

import com.example.DunbarHorizon.social.adapter.in.web.dto.FriendUpdateRequest;
import com.example.DunbarHorizon.social.application.port.in.FriendshipCommandUseCase;
import com.example.DunbarHorizon.social.application.port.in.FriendshipQueryUseCase;
import com.example.DunbarHorizon.social.application.port.in.command.FriendshipUpdateCommand;
import com.example.DunbarHorizon.support.BaseControllerTest;
import com.example.DunbarHorizon.support.WithMockCustomUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FriendshipController.class)
@WithMockCustomUser()
class FriendshipControllerTest extends BaseControllerTest {

    @MockitoBean
    private FriendshipCommandUseCase friendshipCommandUseCase;

    @MockitoBean
    private FriendshipQueryUseCase friendshipQueryUseCase;

    @Test
    @DisplayName("친구 정보를 수정(별명, 소개글 노출 여부 등) 한다")
    void updateFriend_Success() throws Exception {
        // given
        Long friendId = 2L;
        FriendUpdateRequest dto = new FriendUpdateRequest("동기", false, true);

        // when & then
        mockMvc.perform(patch("/api/v1/friends/{friendId}", friendId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNoContent());

        verify(friendshipCommandUseCase).updateFriendship(anyLong(), eq(friendId), any(FriendshipUpdateCommand.class));
    }

    @Test
    @DisplayName("친구 관계를 성공적으로 해제한다")
    void brokeUpWithFriend_Success() throws Exception {
        // given
        Long friendId = 2L;

        // when & then
        mockMvc.perform(delete("/api/v1/friends/{friendId}", friendId))
                .andExpect(status().isNoContent());

        verify(friendshipCommandUseCase).brokeUpWith(anyLong(), eq(friendId));
    }
}