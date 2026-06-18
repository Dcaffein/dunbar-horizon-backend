package com.example.DunbarHorizon.buzz.adapter.in.web;

import com.example.DunbarHorizon.buzz.application.dto.result.BuzzCommentResult;
import com.example.DunbarHorizon.buzz.application.dto.result.BuzzDetailResult;
import com.example.DunbarHorizon.buzz.application.dto.result.BuzzProfileResult;
import com.example.DunbarHorizon.buzz.application.dto.result.BuzzSummaryResult;
import com.example.DunbarHorizon.support.BaseControllerTest;
import com.example.DunbarHorizon.support.WithMockCustomUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BuzzControllerTest extends BaseControllerTest {

    @Nested
    @DisplayName("버즈 상세 조회 응답 검증")
    class GetBuzzDetailResponse {

        private static final Long CURRENT_USER_ID = 1L;

        @Test
        @WithMockCustomUser
        @DisplayName("작성자가 상세 조회하면 응답에 isCreator=true가 포함된다")
        void getBuzzDetail_ByCreator_ResponseContainsIsCreatorTrue() throws Exception {
            BuzzDetailResult detail = new BuzzDetailResult(
                    "buzz-id",
                    new BuzzProfileResult(CURRENT_USER_ID, "작성자", null),
                    "테스트 버즈", List.of(), List.of(), 25L, false, true
            );
            given(buzzQueryUseCase.getBuzzDetail(CURRENT_USER_ID, "buzz-id")).willReturn(detail);

            mockMvc.perform(get("/api/v1/buzzes/buzz-id"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isCreator").value(true));
        }

        @Test
        @WithMockCustomUser
        @DisplayName("수신자가 상세 조회하면 응답에 isCreator=false가 포함된다")
        void getBuzzDetail_ByRecipient_ResponseContainsIsCreatorFalse() throws Exception {
            BuzzDetailResult detail = new BuzzDetailResult(
                    "buzz-id",
                    new BuzzProfileResult(2L, "작성자", null),
                    "테스트 버즈", List.of(), List.of(), 25L, true, false
            );
            given(buzzQueryUseCase.getBuzzDetail(CURRENT_USER_ID, "buzz-id")).willReturn(detail);

            mockMvc.perform(get("/api/v1/buzzes/buzz-id"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isCreator").value(false));
        }

        @Test
        @WithMockCustomUser
        @DisplayName("본인 댓글이면 comments[0].isMine이 true로 내려온다")
        void getBuzzDetail_OwnComment_IsMineTrue() throws Exception {
            BuzzCommentResult comment = new BuzzCommentResult(
                    "c1", new BuzzProfileResult(CURRENT_USER_ID, "작성자", null),
                    "댓글", List.of(), LocalDateTime.now(), true
            );
            BuzzDetailResult detail = new BuzzDetailResult(
                    "buzz-id",
                    new BuzzProfileResult(2L, "버즈작성자", null),
                    "테스트 버즈", List.of(), List.of(comment), 25L, true, false
            );
            given(buzzQueryUseCase.getBuzzDetail(CURRENT_USER_ID, "buzz-id")).willReturn(detail);

            mockMvc.perform(get("/api/v1/buzzes/buzz-id"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.comments[0].isMine").value(true));
        }
    }

    @Nested
    @DisplayName("내가 작성한 버즈 목록 조회")
    class GetSentBuzzes {

        private static final Long CURRENT_USER_ID = 1L;

        @Test
        @WithMockCustomUser
        @DisplayName("GET /api/v1/buzzes/sent 는 작성한 버즈 목록을 반환한다")
        void getSentBuzzes_ReturnsSlice() throws Exception {
            BuzzSummaryResult summary = new BuzzSummaryResult(
                    "buzz-1",
                    new BuzzProfileResult(CURRENT_USER_ID, "작성자", null),
                    "내가 보낸 버즈", List.of(), 0, 10L, false, true
            );
            given(buzzQueryUseCase.getSentBuzzes(eq(CURRENT_USER_ID), any()))
                    .willReturn(new SliceImpl<>(List.of(summary), PageRequest.of(0, 20), false));

            mockMvc.perform(get("/api/v1/buzzes/sent"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].buzzId").value("buzz-1"))
                    .andExpect(jsonPath("$.content[0].isCreator").value(true));
        }
    }

    @Nested
    @DisplayName("버즈 생성 요청 검증")
    class CreateBuzzValidation {

        @Test
        @WithMockCustomUser
        @DisplayName("수신자가 151명이면 400을 반환한다")
        void createBuzz_Fail_RecipientOverLimit() throws Exception {
            String memberIds = LongStream.rangeClosed(1, 151)
                    .mapToObj(String::valueOf)
                    .collect(Collectors.joining(","));

            String body = """
                    {
                      "text": "안녕하세요",
                      "recipient": {
                        "type": "MANUAL",
                        "memberIds": [%s]
                      }
                    }
                    """.formatted(memberIds);

            mockMvc.perform(post("/api/v1/buzzes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validation['recipient.memberIds']").exists());
        }

        @Test
        @WithMockCustomUser
        @DisplayName("수신자가 정확히 150명이면 요청이 서비스까지 전달된다")
        void createBuzz_Success_MaxRecipients() throws Exception {
            String memberIds = LongStream.rangeClosed(1, 150)
                    .mapToObj(String::valueOf)
                    .collect(Collectors.joining(","));

            String body = """
                    {
                      "text": "안녕하세요",
                      "recipient": {
                        "type": "MANUAL",
                        "memberIds": [%s]
                      }
                    }
                    """.formatted(memberIds);

            mockMvc.perform(post("/api/v1/buzzes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated());
        }
    }
}
