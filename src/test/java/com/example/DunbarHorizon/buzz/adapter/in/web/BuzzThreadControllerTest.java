package com.example.DunbarHorizon.buzz.adapter.in.web;

import com.example.DunbarHorizon.buzz.application.dto.command.OpenBuzzThreadCommand;
import com.example.DunbarHorizon.buzz.domain.BuzzThread;
import com.example.DunbarHorizon.buzz.domain.ThreadVisibility;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzThreadAlreadyOpenedException;
import com.example.DunbarHorizon.support.BaseControllerTest;
import com.example.DunbarHorizon.support.WithMockCustomUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BuzzThreadControllerTest extends BaseControllerTest {

    private static final Long CURRENT_USER_ID = 1L;

    @Test
    @WithMockCustomUser
    @DisplayName("스레드를 열면 201을 반환하고 공개 범위와 첫 글을 서비스에 넘긴다")
    void openThread_Returns201() throws Exception {
        String body = """
                { "text": "첫 글", "visibility": "PRIVATE", "imageKeys": ["buzz/a"] }
                """;
        given(buzzThreadService.openThread(any())).willReturn(10L);

        mockMvc.perform(post("/api/v1/buzzes/buzz-1/threads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        verify(buzzThreadService).openThread(new OpenBuzzThreadCommand(
                CURRENT_USER_ID, "buzz-1", ThreadVisibility.PRIVATE, "첫 글", List.of("buzz/a")));
    }

    @Test
    @WithMockCustomUser
    @DisplayName("공개 범위가 없으면 400을 반환한다")
    void openThread_NoVisibility_Returns400() throws Exception {
        mockMvc.perform(post("/api/v1/buzzes/buzz-1/threads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"text\": \"첫 글\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validation.visibility").exists());
    }

    @Test
    @WithMockCustomUser
    @DisplayName("첫 글이 300자를 넘으면 400과 validation.text를 반환한다")
    void openThread_TextOverLimit_Returns400() throws Exception {
        String body = """
                { "text": "%s", "visibility": "PUBLIC" }
                """.formatted("가".repeat(BuzzThread.TEXT_MAX_LENGTH + 1));

        mockMvc.perform(post("/api/v1/buzzes/buzz-1/threads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validation.text").value(BuzzThread.TEXT_LENGTH_MESSAGE));
    }

    @Test
    @WithMockCustomUser
    @DisplayName("같은 공개 범위의 스레드를 다시 열면 409를 반환한다")
    void openThread_Duplicate_Returns409() throws Exception {
        willThrow(new BuzzThreadAlreadyOpenedException("buzz-1", CURRENT_USER_ID, ThreadVisibility.PUBLIC))
                .given(buzzThreadService).openThread(any());

        mockMvc.perform(post("/api/v1/buzzes/buzz-1/threads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"text\": \"첫 글\", \"visibility\": \"PUBLIC\" }"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("BUZZ_THREAD_ALREADY_OPENED"));
    }
}
