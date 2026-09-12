package com.example.DunbarHorizon.global.exception;

import com.example.DunbarHorizon.account.domain.exception.InvalidCredentialsException;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzErrorCode;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzInvalidStateException;
import com.example.DunbarHorizon.flag.domain.exception.FlagErrorCode;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagFullCapacityException;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagInvalidStatusException;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 문구가 두 청중으로 갈렸는지 검증한다.
 *
 * <pre>
 * 응답   getUserMessage()  값이 섞이지 않은 완성된 문장
 * 로그   getMessage()      코드 + 컨텍스트. 사용자 문장이 없다
 * </pre>
 */
class ErrorMessageSeparationTest {

    @Test
    @DisplayName("응답 문구에는 내부 ID가 섞이지 않는다")
    void userMessage_HasNoIdentifiers() {
        // given
        FlagNotFoundException e = new FlagNotFoundException(99999999L);

        // when & then
        assertThat(e.getUserMessage())
                .isEqualTo("요청하신 깃발을 찾을 수 없습니다.")
                .doesNotContain("99999999");
    }

    @Test
    @DisplayName("로그 문구는 코드와 컨텍스트로만 이루어진다")
    void logMessage_IsCodeAndContext() {
        // given
        FlagNotFoundException e = new FlagNotFoundException(99999999L);

        // when & then
        assertThat(e.getMessage())
                .isEqualTo("FLAG_NOT_FOUND {flagId=99999999}")
                .doesNotContain("요청하신");
    }

    @Test
    @DisplayName("인자 없이 던지던 예외도 판정에 쓰인 값을 로그에 남긴다")
    void logMessage_CarriesDecisionValues() {
        // given - 예전에는 어느 깃발이었는지조차 남지 않았다
        FlagFullCapacityException e = new FlagFullCapacityException(331L, 8, 8);

        // when & then
        assertThat(e.getMessage())
                .isEqualTo("FLAG_FULL_CAPACITY {flagId=331, capacity=8, participantCount=8}");
        assertThat(e.getUserMessage()).isEqualTo("정원이 가득 찬 깃발입니다.");
    }

    @Test
    @DisplayName("컨텍스트가 없으면 로그 문구는 코드뿐이다")
    void logMessage_CodeOnlyWithoutContext() {
        // given
        FlagInvalidStatusException e = new FlagInvalidStatusException(FlagErrorCode.FLAG_HOST_REQUIRED);

        // when & then
        assertThat(e.getMessage()).isEqualTo("FLAG_HOST_REQUIRED");
    }

    @Test
    @DisplayName("같은 예외 타입이라도 조건마다 코드가 다르다")
    void splitCodes_DistinguishConditions() {
        // given - 예전에는 셋 다 FLAG_INVALID_STATUS였고 한국어 문장만이 구분했다
        FlagInvalidStatusException notRecruiting =
                new FlagInvalidStatusException(FlagErrorCode.FLAG_NOT_RECRUITING);
        FlagInvalidStatusException alreadyStarted =
                new FlagInvalidStatusException(FlagErrorCode.FLAG_ALREADY_STARTED);
        FlagInvalidStatusException alreadyEnded =
                new FlagInvalidStatusException(FlagErrorCode.FLAG_ALREADY_ENDED);

        // when & then
        assertThat(notRecruiting.getCode()).isEqualTo("FLAG_NOT_RECRUITING");
        assertThat(alreadyStarted.getCode()).isEqualTo("FLAG_ALREADY_STARTED");
        assertThat(alreadyEnded.getCode()).isEqualTo("FLAG_ALREADY_ENDED");
        assertThat(notRecruiting.getUserMessage())
                .isNotEqualTo(alreadyStarted.getUserMessage())
                .isNotEqualTo(alreadyEnded.getUserMessage());
    }

    @Test
    @DisplayName("잘못 분류돼 있던 status가 교정됐다")
    void correctedStatuses() {
        // given - 굵은 코드에 여러 조건을 몰아넣은 탓에 드러나지 않던 분류 오류
        assertThat(BuzzErrorCode.BUZZ_COMMENT_NOT_FOUND.status())
                .as("댓글을 못 찾은 것은 400이 아니라 404다")
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(FlagErrorCode.FLAG_HOST_REQUIRED.status())
                .as("필수값 누락은 409가 아니라 400이다")
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(FlagErrorCode.FLAG_SCHEDULE_REQUIRED.status())
                .as("일정 검증 실패는 409가 아니라 400이다")
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("로그에는 조사에 필요한 값이 남지만 응답에는 나가지 않는다")
    void context_StaysInLogOnly() {
        // given - 계정 열거를 막으려면 응답은 같아야 하고, 조사하려면 로그는 갈려야 한다
        InvalidCredentialsException e = new InvalidCredentialsException("user@example.com");

        // when & then
        assertThat(e.getMessage()).contains("user@example.com");
        assertThat(e.getUserMessage())
                .isEqualTo("이메일 또는 비밀번호가 올바르지 않습니다.")
                .doesNotContain("user@example.com");
    }

    @Test
    @DisplayName("같은 사실을 말하는 서로 다른 동작은 한 코드로 모인다")
    void sameFact_SharesOneCode() {
        // given - 만료된 버즈에 댓글을 달거나 수정하거나 삭제하는 것은 사용자에게 한 가지 사실이다
        BuzzInvalidStateException expired =
                new BuzzInvalidStateException(BuzzErrorCode.BUZZ_EXPIRED, ErrorContext.of("buzzId", "abc"));

        // when & then
        assertThat(expired.getCode()).isEqualTo("BUZZ_EXPIRED");
        assertThat(expired.getUserMessage()).isEqualTo("만료된 버즈입니다.");
        assertThat(expired.getMessage()).isEqualTo("BUZZ_EXPIRED {buzzId=abc}");
    }
}
