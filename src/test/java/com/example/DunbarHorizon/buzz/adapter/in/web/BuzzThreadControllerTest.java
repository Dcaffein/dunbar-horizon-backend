package com.example.DunbarHorizon.buzz.adapter.in.web;

import com.example.DunbarHorizon.buzz.application.dto.command.CreateBuzzThreadCommand;
import com.example.DunbarHorizon.buzz.application.dto.command.UpdateBuzzCommentCommand;
import com.example.DunbarHorizon.buzz.application.dto.command.UpdateBuzzThreadCommand;
import com.example.DunbarHorizon.buzz.application.dto.command.WriteBuzzCommentCommand;
import com.example.DunbarHorizon.buzz.domain.BuzzComment;
import com.example.DunbarHorizon.buzz.domain.BuzzThread;
import com.example.DunbarHorizon.buzz.domain.ThreadVisibility;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzThreadAlreadyExistsException;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BuzzThreadControllerTest extends BaseControllerTest {

    private static final Long CURRENT_USER_ID = 1L;

    @Test
    @WithMockCustomUser
    @DisplayName("스레드를 생성하면 201을 반환하고 공개 범위와 첫 글을 서비스에 넘긴다")
    void createThread_Returns201() throws Exception {
        String body = """
                { "text": "첫 글", "visibility": "PRIVATE", "imageKeys": ["buzz/a"] }
                """;
        given(buzzThreadService.createThread(any())).willReturn(10L);

        mockMvc.perform(post("/api/v1/buzzes/1/threads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        verify(buzzThreadService).createThread(new CreateBuzzThreadCommand(
                CURRENT_USER_ID, 1L, ThreadVisibility.PRIVATE, "첫 글", List.of("buzz/a")));
    }

    @Test
    @WithMockCustomUser
    @DisplayName("공개 범위가 없으면 400을 반환한다")
    void createThread_NoVisibility_Returns400() throws Exception {
        mockMvc.perform(post("/api/v1/buzzes/1/threads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"text\": \"첫 글\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validation.visibility").exists());
    }

    @Test
    @WithMockCustomUser
    @DisplayName("첫 글이 300자를 넘으면 400과 validation.text를 반환한다")
    void createThread_TextOverLimit_Returns400() throws Exception {
        String body = """
                { "text": "%s", "visibility": "PUBLIC" }
                """.formatted("가".repeat(BuzzThread.TEXT_MAX_LENGTH + 1));

        mockMvc.perform(post("/api/v1/buzzes/1/threads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validation.text").value(BuzzThread.TEXT_LENGTH_MESSAGE));
    }

    @Test
    @WithMockCustomUser
    @DisplayName("답글을 쓰면 201을 반환하고 스레드와 본문을 서비스에 넘긴다")
    void writeComment_Returns201() throws Exception {
        mockMvc.perform(post("/api/v1/buzzes/1/threads/10/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"text\": \"답글\" }"))
                .andExpect(status().isCreated());

        verify(buzzCommentService).writeComment(new WriteBuzzCommentCommand(
                CURRENT_USER_ID, 1L, 10L, "답글", List.of()));
    }

    @Test
    @WithMockCustomUser
    @DisplayName("답글이 300자를 넘으면 400과 validation.text를 반환한다")
    void writeComment_TextOverLimit_Returns400() throws Exception {
        String body = """
                { "text": "%s" }
                """.formatted("가".repeat(BuzzComment.TEXT_MAX_LENGTH + 1));

        mockMvc.perform(post("/api/v1/buzzes/1/threads/10/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validation.text").value(BuzzComment.TEXT_LENGTH_MESSAGE));
    }

    @Test
    @WithMockCustomUser
    @DisplayName("첫 글 수정과 답글 수정은 204를 반환한다")
    void update_Returns204() throws Exception {
        mockMvc.perform(patch("/api/v1/buzzes/1/threads/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"text\": \"수정한 글\", \"imageKeys\": [\"buzz/b\"] }"))
                .andExpect(status().isNoContent());
        mockMvc.perform(patch("/api/v1/buzzes/1/threads/10/comments/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"text\": \"수정한 답글\" }"))
                .andExpect(status().isNoContent());

        verify(buzzThreadService).updateThread(new UpdateBuzzThreadCommand(
                CURRENT_USER_ID, 1L, 10L, "수정한 글", List.of("buzz/b")));
        verify(buzzCommentService).updateComment(new UpdateBuzzCommentCommand(
                CURRENT_USER_ID, 1L, 10L, 100L, "수정한 답글", List.of()));
    }

    @Test
    @WithMockCustomUser
    @DisplayName("스레드 삭제와 답글 삭제는 204를 반환한다")
    void delete_Returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/buzzes/1/threads/10"))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/v1/buzzes/1/threads/10/comments/100"))
                .andExpect(status().isNoContent());

        verify(buzzThreadService).deleteThread(CURRENT_USER_ID, 1L, 10L);
        verify(buzzCommentService).deleteComment(CURRENT_USER_ID, 1L, 10L, 100L);
    }

    @Test
    @WithMockCustomUser
    @DisplayName("같은 공개 범위의 스레드를 다시 생성하면 409를 반환한다")
    void createThread_Duplicate_Returns409() throws Exception {
        willThrow(new BuzzThreadAlreadyExistsException(1L, CURRENT_USER_ID, ThreadVisibility.PUBLIC))
                .given(buzzThreadService).createThread(any());

        mockMvc.perform(post("/api/v1/buzzes/1/threads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"text\": \"첫 글\", \"visibility\": \"PUBLIC\" }"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("BUZZ_THREAD_ALREADY_EXISTS"));
    }
}
