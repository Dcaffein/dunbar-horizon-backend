package com.example.DunbarHorizon.buzz.adapter.in.web;

import com.example.DunbarHorizon.buzz.application.port.in.BuzzCommandUseCase;
import com.example.DunbarHorizon.buzz.application.port.in.BuzzQueryUseCase;
import com.example.DunbarHorizon.support.BaseControllerTest;
import com.example.DunbarHorizon.support.WithMockCustomUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BuzzController.class)
class BuzzControllerTest extends BaseControllerTest {

    @MockitoBean BuzzCommandUseCase buzzCommandUseCase;
    @MockitoBean BuzzQueryUseCase buzzQueryUseCase;

    @Nested
    @DisplayName("버즈 생성 요청 검증")
    class CreateBuzzValidation {

        @Test
        @WithMockCustomUser
        @DisplayName("수신자가 151명이면 400을 반환한다")
        void createBuzz_Fail_RecipientOverLimit() throws Exception {
            // given
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

            MockMultipartFile requestPart = new MockMultipartFile(
                    "request", "", MediaType.APPLICATION_JSON_VALUE, body.getBytes());

            // when & then
            mockMvc.perform(multipart("/api/v1/buzzes")
                            .file(requestPart))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validation['recipient.memberIds']").exists());
        }

        @Test
        @WithMockCustomUser
        @DisplayName("수신자가 정확히 150명이면 요청이 서비스까지 전달된다")
        void createBuzz_Success_MaxRecipients() throws Exception {
            // given
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

            MockMultipartFile requestPart = new MockMultipartFile(
                    "request", "", MediaType.APPLICATION_JSON_VALUE, body.getBytes());

            // when & then
            mockMvc.perform(multipart("/api/v1/buzzes")
                            .file(requestPart))
                    .andExpect(status().isCreated());
        }
    }
}
