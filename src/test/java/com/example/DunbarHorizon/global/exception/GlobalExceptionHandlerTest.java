package com.example.DunbarHorizon.global.exception;

import com.example.DunbarHorizon.flag.domain.flag.exception.FlagFullCapacityException;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagNotFoundException;
import com.example.DunbarHorizon.support.BaseControllerTest;
import com.example.DunbarHorizon.support.WithMockCustomUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 에러 응답의 판별자가 클래스명이 아니라 코드 상수인지 검증한다.
 *
 * <p>기존 예외 테스트는 예외 타입으로 단언하므로 클래스명을 바꿔도 통과한다.
 * 여기서는 응답에 실린 문자열을 직접 단언해 계약을 고정한다.
 */
@WithMockCustomUser
class GlobalExceptionHandlerTest extends BaseControllerTest {

    @Test
    @DisplayName("도메인 예외의 error는 클래스명이 아니라 코드 상수다")
    void domainException_ReturnsErrorCode() throws Exception {
        // given
        willThrow(new FlagFullCapacityException(1L, 8, 8))
                .given(flagParticipationUseCase).participateInFlag(anyLong(), anyLong());

        // when & then
        mockMvc.perform(post("/api/v1/flags/1/participants"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("FLAG_FULL_CAPACITY"))
                .andExpect(jsonPath("$.message").value("정원이 가득 찬 깃발입니다."));
    }

    @Test
    @DisplayName("응답 어디에도 ~Exception 형태의 클래스명이 남지 않는다")
    void domainException_DoesNotExposeClassName() throws Exception {
        // given
        willThrow(new FlagNotFoundException(331L))
                .given(flagParticipationUseCase).participateInFlag(anyLong(), anyLong());

        // when & then
        mockMvc.perform(post("/api/v1/flags/1/participants"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("FLAG_NOT_FOUND"))
                .andExpect(jsonPath("$.error").value(not(containsString("Exception"))))
                .andExpect(jsonPath("$.message").value("요청하신 깃발을 찾을 수 없습니다."))
                .andExpect(jsonPath("$.message").value(not(containsString("331"))));
    }

    @Test
    @DisplayName("경로변수 타입이 맞지 않으면 500이 아니라 400을 반환한다")
    void typeMismatch_Returns400() throws Exception {
        // when & then
        mockMvc.perform(post("/api/v1/flags/abc/participants"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_PARAMETER_TYPE"));
    }

    @Test
    @DisplayName("타입 불일치 응답은 사용자가 보낸 값을 되비추지 않는다")
    void typeMismatch_DoesNotEchoUserInput() throws Exception {
        // when & then
        mockMvc.perform(post("/api/v1/flags/abc/participants"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(not(containsString("abc"))));
    }

    @Test
    @DisplayName("지원하지 않는 Content-Type이면 500이 아니라 415를 반환한다")
    void unsupportedMediaType_Returns415() throws Exception {
        // when & then
        mockMvc.perform(post("/api/v1/flags")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("title=테스트"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.error").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    @Test
    @DisplayName("지원하지 않는 메서드면 405와 METHOD_NOT_ALLOWED를 반환한다")
    void methodNotSupported_Returns405() throws Exception {
        // when & then
        mockMvc.perform(put("/api/v1/flags/1/participants/me"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.error").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    @DisplayName("JSON 형식이 깨지면 400과 INVALID_JSON_FORMAT을 반환한다")
    void malformedJson_Returns400() throws Exception {
        // when & then
        mockMvc.perform(post("/api/v1/flags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_JSON_FORMAT"));
    }

    @Test
    @DisplayName("검증 실패는 INVALID_INPUT과 validation 맵을 함께 반환한다")
    void validationFailure_KeepsValidationMap() throws Exception {
        // given
        String body = """
                {
                  "title": "",
                  "description": "설명",
                  "capacity": 10,
                  "startDateTime": "2030-12-01T10:00:00",
                  "endDateTime": "2030-12-01T12:00:00"
                }
                """;

        // when & then
        mockMvc.perform(post("/api/v1/flags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.validation.title").exists());
    }
}
