package com.example.DunbarHorizon.buzz.adapter.in.web;

import com.example.DunbarHorizon.support.BaseControllerTest;
import com.example.DunbarHorizon.support.WithMockCustomUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BuzzControllerTest extends BaseControllerTest {

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
