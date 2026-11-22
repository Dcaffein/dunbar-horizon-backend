package com.example.DunbarHorizon.buzz.domain.model;

import com.example.DunbarHorizon.buzz.domain.Buzz;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzAccessDeniedException;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzInvalidStateException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BuzzTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 22, 12, 0);

    private final Long creatorId = 1L;
    private final Long recipientId = 2L;
    private final Long strangerId = 99L;

    private Buzz buzz() {
        return Buzz.create(creatorId, "Hello", List.of(), Set.of(recipientId), NOW);
    }

    @Nested
    @DisplayName("버즈 생성")
    class CreationTest {

        @Test
        @DisplayName("공개 대상이 한 명도 없으면 생성에 실패한다")
        void create_Fail_NoAudience() {
            assertThatThrownBy(() -> Buzz.create(creatorId, "Hello", List.of(), Set.of(), NOW))
                    .isInstanceOf(BuzzInvalidStateException.class);
        }

        @Test
        @DisplayName("공개 대상이 151명이어도 인원수 상한 없이 생성된다")
        void create_Success_AudienceOver150() {
            // given
            Set<Long> audienceIds = LongStream.rangeClosed(2, 152).boxed().collect(Collectors.toSet());

            // when & then
            Buzz buzz = Buzz.create(creatorId, "Hello", List.of(), audienceIds, NOW);
            assertThat(buzz.getCreatorId()).isEqualTo(creatorId);
        }

        @Test
        @DisplayName("본문이 비어 있으면 생성에 실패한다")
        void create_Fail_BlankText() {
            assertThatThrownBy(() -> Buzz.create(creatorId, "  ", List.of(), Set.of(recipientId), NOW))
                    .isInstanceOf(BuzzInvalidStateException.class);
        }

        @Test
        @DisplayName("본문이 1000자를 초과하면 생성에 실패한다")
        void create_Fail_TooLongText() {
            String tooLong = "가".repeat(Buzz.TEXT_MAX_LENGTH + 1);

            assertThatThrownBy(() -> Buzz.create(creatorId, tooLong, List.of(), Set.of(recipientId), NOW))
                    .isInstanceOf(BuzzInvalidStateException.class);
        }

        @Test
        @DisplayName("본문이 정확히 1000자이면 생성에 성공한다")
        void create_Success_MaxLengthText() {
            String boundary = "가".repeat(Buzz.TEXT_MAX_LENGTH);

            Buzz buzz = Buzz.create(creatorId, boundary, List.of(), Set.of(recipientId), NOW);

            assertThat(buzz.getText()).hasSize(Buzz.TEXT_MAX_LENGTH);
        }

        @Test
        @DisplayName("생성 시각에서 6시간 뒤에 만료되고 이미지 키를 그대로 가진다")
        void create_Success_FieldsInitialized() {
            Buzz buzz = Buzz.create(creatorId, "Hello", List.of("buzz/a"), Set.of(recipientId), NOW);

            assertThat(buzz.getCreatedAt()).isEqualTo(NOW);
            assertThat(buzz.getExpiresAt()).isEqualTo(NOW.plusHours(6));
            assertThat(buzz.getImageKeys()).containsExactly("buzz/a");
        }

        @Test
        @DisplayName("이미지 키가 null이면 빈 목록으로 생성된다")
        void create_Success_NullImageKeys() {
            Buzz buzz = Buzz.create(creatorId, "Hello", null, Set.of(recipientId), NOW);

            assertThat(buzz.getImageKeys()).isEmpty();
        }
    }

    @Nested
    @DisplayName("버즈 수정")
    class UpdateTest {

        @Test
        @DisplayName("게시자는 만료 전에 본문과 이미지 키를 고치고 수정 시각이 남는다")
        void update_ByCreator() {
            // given
            Buzz buzz = buzz();
            LocalDateTime editedAt = NOW.plusHours(1);

            // when
            buzz.update(creatorId, "고친 본문", List.of("buzz/b"), editedAt);

            // then
            assertThat(buzz.getText()).isEqualTo("고친 본문");
            assertThat(buzz.getImageKeys()).containsExactly("buzz/b");
            assertThat(buzz.getEditedAt()).isEqualTo(editedAt);
        }

        @Test
        @DisplayName("새로 만든 버즈는 수정 시각이 없다")
        void create_NoEditedAt() {
            assertThat(buzz().getEditedAt()).isNull();
        }

        @Test
        @DisplayName("게시자가 아니면 수정할 수 없다")
        void update_ByNonCreator_Denied() {
            assertThatThrownBy(() -> buzz().update(recipientId, "고친 본문", List.of(), NOW.plusHours(1)))
                    .isInstanceOf(BuzzAccessDeniedException.class);
        }

        @Test
        @DisplayName("고친 본문도 길이 규칙을 지켜야 한다")
        void update_InvalidText_Rejected() {
            assertThatThrownBy(() -> buzz().update(creatorId, " ", List.of(), NOW.plusHours(1)))
                    .isInstanceOf(BuzzInvalidStateException.class);
            assertThatThrownBy(() -> buzz().update(creatorId, "가".repeat(Buzz.TEXT_MAX_LENGTH + 1), List.of(), NOW.plusHours(1)))
                    .isInstanceOf(BuzzInvalidStateException.class);
        }
    }

    @Nested
    @DisplayName("만료 판단")
    class ExpiryTest {

        @Test
        @DisplayName("만료 시각 이전이면 만료되지 않았다")
        void isExpired_False_BeforeExpiresAt() {
            assertThat(buzz().isExpired(NOW.plusHours(6).minusNanos(1))).isFalse();
        }

        @Test
        @DisplayName("만료 시각과 같으면 만료됐다")
        void isExpired_True_AtExpiresAt() {
            assertThat(buzz().isExpired(NOW.plusHours(6))).isTrue();
        }
    }

    @Nested
    @DisplayName("접근 및 삭제 권한")
    class AccessValidationTest {

        @Test
        @DisplayName("공개 대상 멤버는 버즈에 접근할 수 있다")
        void validateAccess_Success_AudienceMember() {
            buzz().validateAccess(recipientId, true);
        }

        @Test
        @DisplayName("게시자는 공개 대상이 아니어도 접근할 수 있다")
        void validateAccess_Success_Creator() {
            buzz().validateAccess(creatorId, false);
        }

        @Test
        @DisplayName("게시자도 공개 대상 멤버도 아니면 예외가 발생한다")
        void validateAccess_Fail_Stranger() {
            assertThatThrownBy(() -> buzz().validateAccess(strangerId, false))
                    .isInstanceOf(BuzzAccessDeniedException.class);
        }

        @Test
        @DisplayName("게시자가 삭제하면 삭제 시각이 남는다")
        void delete_ByCreator_MarksDeleted() {
            // given
            Buzz buzz = buzz();

            // when
            buzz.delete(creatorId, NOW.plusHours(1));

            // then
            assertThat(buzz.getDeletedAt()).isEqualTo(NOW.plusHours(1));
        }

        @Test
        @DisplayName("게시자가 아닌 사람이 삭제하려 하면 예외가 발생하고 삭제 표시가 남지 않는다")
        void delete_ByNonCreator_Denied() {
            // given
            Buzz buzz = buzz();

            // when & then
            assertThatThrownBy(() -> buzz.delete(recipientId, NOW.plusHours(1)))
                    .isInstanceOf(BuzzAccessDeniedException.class);
            assertThat(buzz.getDeletedAt()).isNull();
        }

        @Test
        @DisplayName("isCreator는 게시자 ID에만 true다")
        void isCreator_OnlyCreator() {
            assertThat(buzz().isCreator(creatorId)).isTrue();
            assertThat(buzz().isCreator(recipientId)).isFalse();
        }
    }
}
