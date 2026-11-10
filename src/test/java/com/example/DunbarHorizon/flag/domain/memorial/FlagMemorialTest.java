package com.example.DunbarHorizon.flag.domain.memorial;

import com.example.DunbarHorizon.flag.domain.exception.FlagErrorCode;
import com.example.DunbarHorizon.flag.domain.flag.Flag;
import com.example.DunbarHorizon.flag.domain.flag.FlagSchedule;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagAuthorizationException;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagInvalidStatusException;
import com.example.DunbarHorizon.flag.domain.memorial.exception.FlagMemorialAuthorizationException;
import com.example.DunbarHorizon.flag.domain.memorial.exception.FlagMemorialInvalidContentException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FlagMemorialTest {

    private static final Long FLAG_ID = 1L;
    private static final Long HOST_ID = 10L;
    private static final Long WRITER_ID = 20L;
    private static final Long OTHER_ID = 99L;

    private Flag flagWith(FlagSchedule schedule) {
        Flag flag = Flag.create(HOST_ID, "모임", "설명", 10, schedule);
        ReflectionTestUtils.setField(flag, "id", FLAG_ID);
        return flag;
    }

    private Flag endedFlag() {
        LocalDateTime now = LocalDateTime.now();
        return flagWith(FlagSchedule.of(now.minusHours(3), now.minusHours(2), now.minusHours(1)));
    }

    @Test
    @DisplayName("종료된 Flag의 호스트는 참여자가 아니어도 후기를 생성할 수 있다")
    void create_Success_Host() {
        // given
        Flag flag = endedFlag();

        // when
        FlagMemorial memorial = FlagMemorial.create(flag, HOST_ID, "후기", false);

        // then
        assertThat(memorial.getFlagId()).isEqualTo(FLAG_ID);
        assertThat(memorial.getWriterId()).isEqualTo(HOST_ID);
        assertThat(memorial.getContent()).isEqualTo("후기");
        assertThat(flag.isPreserved()).isFalse();
        assertThat(flag.getDomainEvents()).isEmpty();
    }

    @Test
    @DisplayName("종료된 Flag의 참여자는 후기를 생성할 수 있다")
    void create_Success_Participant() {
        // when
        FlagMemorial memorial = FlagMemorial.create(endedFlag(), WRITER_ID, "후기", true);

        // then
        assertThat(memorial.getWriterId()).isEqualTo(WRITER_ID);
    }

    @Test
    @DisplayName("호스트도 참여자도 아닌 사용자의 후기 생성은 거절된다")
    void create_Fail_Stranger() {
        assertThatThrownBy(() -> FlagMemorial.create(endedFlag(), OTHER_ID, "후기", false))
                .isInstanceOfSatisfying(FlagMemorialAuthorizationException.class,
                        exception -> assertThat(exception.getCode())
                                .isEqualTo(FlagErrorCode.FLAG_MEMORIAL_PARTICIPANT_ONLY.code()));
    }

    @Test
    @DisplayName("모집 중인 Flag에는 호스트도 후기를 생성할 수 없다")
    void create_Fail_RecruitingFlag() {
        // given
        LocalDateTime now = LocalDateTime.now();
        Flag flag = flagWith(FlagSchedule.of(now.plusHours(1), now.plusHours(2), now.plusHours(3)));

        // when & then
        assertThatThrownBy(() -> FlagMemorial.create(flag, HOST_ID, "후기", false))
                .isInstanceOfSatisfying(FlagInvalidStatusException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(FlagErrorCode.FLAG_NOT_ENDED.code()));
    }

    @Test
    @DisplayName("진행 중인 Flag에는 참여자도 후기를 생성할 수 없다")
    void create_Fail_InActivityFlag() {
        // given
        LocalDateTime now = LocalDateTime.now();
        Flag flag = flagWith(FlagSchedule.of(now.minusHours(2), now.minusHours(1), now.plusHours(1)));

        // when & then
        assertThatThrownBy(() -> FlagMemorial.create(flag, WRITER_ID, "후기", true))
                .isInstanceOf(FlagInvalidStatusException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\n", "\t"})
    @DisplayName("후기 내용이 없거나 공백뿐이면 생성할 수 없다")
    void create_Fail_BlankContent(String content) {
        assertThatThrownBy(() -> FlagMemorial.create(endedFlag(), HOST_ID, content, false))
                .isInstanceOf(FlagMemorialInvalidContentException.class);
    }

    @Test
    @DisplayName("후기 내용은 최대 길이까지 허용한다")
    void create_Success_MaxLengthContent() {
        // given
        String content = "가".repeat(FlagMemorial.CONTENT_MAX_LENGTH);

        // when
        FlagMemorial memorial = FlagMemorial.create(endedFlag(), HOST_ID, content, false);

        // then
        assertThat(memorial.getContent()).isEqualTo(content);
    }

    @Test
    @DisplayName("최대 길이를 넘는 후기는 생성할 수 없다")
    void create_Fail_TooLongContent() {
        assertThatThrownBy(() -> FlagMemorial.create(endedFlag(), HOST_ID,
                "가".repeat(FlagMemorial.CONTENT_MAX_LENGTH + 1), false))
                .isInstanceOf(FlagMemorialInvalidContentException.class);
    }

    @Test
    @DisplayName("작성자는 후기 내용을 수정할 수 있다")
    void updateContent_Success_Writer() {
        // given
        FlagMemorial memorial = FlagMemorial.create(endedFlag(), WRITER_ID, "기존 후기", true);

        // when
        memorial.updateContent(WRITER_ID, "수정한 후기");

        // then
        assertThat(memorial.getContent()).isEqualTo("수정한 후기");
    }

    @Test
    @DisplayName("작성자가 아니면 호스트도 후기 내용을 수정할 수 없다")
    void updateContent_Fail_NonWriterHost() {
        // given
        FlagMemorial memorial = FlagMemorial.create(endedFlag(), WRITER_ID, "기존 후기", true);

        // when & then
        assertThatThrownBy(() -> memorial.updateContent(HOST_ID, "수정 시도"))
                .isInstanceOf(FlagAuthorizationException.class);
        assertThat(memorial.getContent()).isEqualTo("기존 후기");
    }

    @Test
    @DisplayName("유효하지 않은 내용으로 수정하면 기존 후기가 유지된다")
    void updateContent_Fail_BlankContent() {
        // given
        FlagMemorial memorial = FlagMemorial.create(endedFlag(), WRITER_ID, "기존 후기", true);

        // when & then
        assertThatThrownBy(() -> memorial.updateContent(WRITER_ID, " "))
                .isInstanceOf(FlagMemorialInvalidContentException.class);
        assertThat(memorial.getContent()).isEqualTo("기존 후기");
    }

    @Test
    @DisplayName("작성자는 자신의 후기를 삭제할 수 있다")
    void validateDeletion_Success_Writer() {
        // given
        FlagMemorial memorial = FlagMemorial.create(endedFlag(), WRITER_ID, "후기", true);

        // when & then
        assertThatCode(() -> memorial.validateDeletion(WRITER_ID)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("작성자가 아니면 호스트도 후기를 삭제할 수 없다")
    void validateDeletion_Fail_NonWriterHost() {
        // given
        FlagMemorial memorial = FlagMemorial.create(endedFlag(), WRITER_ID, "후기", true);

        // when & then
        assertThatThrownBy(() -> memorial.validateDeletion(HOST_ID))
                .isInstanceOfSatisfying(FlagAuthorizationException.class,
                        exception -> assertThat(exception.getCode())
                                .isEqualTo(FlagErrorCode.FLAG_MEMORIAL_AUTHOR_ONLY.code()));
    }
}
