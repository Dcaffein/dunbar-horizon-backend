package com.example.DunbarHorizon.flag.adapter.in.web;

import com.example.DunbarHorizon.flag.application.port.in.FlagCommentCommandUseCase;
import com.example.DunbarHorizon.flag.application.port.in.FlagCommentQueryUseCase;
import com.example.DunbarHorizon.support.BaseControllerTest;
import com.example.DunbarHorizon.support.WithMockCustomUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FlagCommentController.class)
@WithMockCustomUser
class FlagCommentControllerTest extends BaseControllerTest {

    @MockitoBean private FlagCommentCommandUseCase flagCommentCommandUseCase;
    @MockitoBean private FlagCommentQueryUseCase commentQueryUseCase;

    private static final Long CURRENT_USER_ID = 1L;

    @Test
    @DisplayName("댓글 트리 조회 시 200을 반환하고 getCommentTree()를 호출한다")
    void getComments_Returns200() throws Exception {
        // given
        given(commentQueryUseCase.getCommentTree(1L, CURRENT_USER_ID)).willReturn(List.of());

        // when / then
        mockMvc.perform(get("/api/v1/flags/1/comments"))
                .andExpect(status().isOk());

        verify(commentQueryUseCase).getCommentTree(1L, CURRENT_USER_ID);
    }

    @Test
    @DisplayName("댓글 수 조회 시 200을 반환하고 getCommentCount()를 호출한다")
    void getCommentCount_Returns200() throws Exception {
        // given
        given(commentQueryUseCase.getCommentCount(1L)).willReturn(5L);

        // when / then
        mockMvc.perform(get("/api/v1/flags/1/comments/count"))
                .andExpect(status().isOk());

        verify(commentQueryUseCase).getCommentCount(1L);
    }

    @Test
    @DisplayName("루트 댓글 생성 시 201을 반환하고 createRootComment()를 호출한다")
    void createRootComment_Returns201() throws Exception {
        // given
        given(flagCommentCommandUseCase.createRootComment(eq(1L), eq(CURRENT_USER_ID), anyString(), anyBoolean()))
                .willReturn(10L);
        String body = """
                {"content": "댓글 내용", "isPrivate": false}
                """;

        // when / then
        mockMvc.perform(post("/api/v1/flags/1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        verify(flagCommentCommandUseCase).createRootComment(eq(1L), eq(CURRENT_USER_ID), eq("댓글 내용"), eq(false));
    }

    @Test
    @DisplayName("답글 생성 시 201을 반환하고 createReply()를 호출한다")
    void createReply_Returns201() throws Exception {
        // given
        given(flagCommentCommandUseCase.createReply(eq(1L), eq(CURRENT_USER_ID), anyString(), anyBoolean()))
                .willReturn(11L);
        String body = """
                {"content": "답글 내용", "isPrivate": false}
                """;

        // when / then
        mockMvc.perform(post("/api/v1/comments/1/replies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        verify(flagCommentCommandUseCase).createReply(eq(1L), eq(CURRENT_USER_ID), eq("답글 내용"), eq(false));
    }

    @Test
    @DisplayName("댓글 수정 시 200을 반환하고 updateComment()를 호출한다")
    void updateComment_Returns200() throws Exception {
        // given
        String body = """
                {"content": "수정된 내용", "isPrivate": false}
                """;

        // when / then
        mockMvc.perform(patch("/api/v1/comments/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        verify(flagCommentCommandUseCase).updateComment(1L, CURRENT_USER_ID, "수정된 내용", false);
    }

    @Test
    @DisplayName("댓글 삭제 시 204를 반환하고 deleteComment()를 호출한다")
    void deleteComment_Returns204() throws Exception {
        // when / then
        mockMvc.perform(delete("/api/v1/comments/1"))
                .andExpect(status().isNoContent());

        verify(flagCommentCommandUseCase).deleteComment(1L, CURRENT_USER_ID);
    }
}
