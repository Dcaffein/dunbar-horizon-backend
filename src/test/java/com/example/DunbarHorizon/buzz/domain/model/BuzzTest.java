package com.example.DunbarHorizon.buzz.domain.model;

import com.example.DunbarHorizon.buzz.domain.Buzz;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzAccessDeniedException;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzInvalidStateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BuzzTest {

    private final Long creatorId = 1L;
    private final String creatorNickname = "방장";
    private final String creatorProfile = "boss.png";

    private final Long recipientId = 2L;

    private final Long strangerId = 99L;

    @Nested
    @DisplayName("버즈 생성 및 초기화 테스트")
    class CreationTest {
        @Test
        @DisplayName("수신자가 한 명도 없으면 생성에 실패한다")
        void createBuzz_Fail_NoRecipients() {
            assertThatThrownBy(() -> Buzz.builder()
                    .creatorId(creatorId)
                    .creatorNickname(creatorNickname)
                    .creatorProfileImageUrl(creatorProfile)
                    .text("Hello")
                    .recipientIds(List.of())
                    .build())
                    .isInstanceOf(BuzzInvalidStateException.class);
        }

        @Test
        @DisplayName("수신자가 150명을 초과하면 생성에 실패한다")
        void createBuzz_Fail_TooManyRecipients() {
            // given
            List<Long> recipients = LongStream.rangeClosed(1, 151).boxed().toList();

            // when & then
            assertThatThrownBy(() -> Buzz.builder()
                    .creatorId(creatorId)
                    .creatorNickname(creatorNickname)
                    .creatorProfileImageUrl(creatorProfile)
                    .text("Hello")
                    .recipientIds(recipients)
                    .build())
                    .isInstanceOf(BuzzInvalidStateException.class);
        }

        @Test
        @DisplayName("수신자가 정확히 150명이면 생성에 성공한다")
        void createBuzz_Success_MaxRecipients() {
            // given
            List<Long> recipients = LongStream.rangeClosed(1, 150).boxed().toList();

            // when & then
            Buzz buzz = Buzz.builder()
                    .creatorId(creatorId)
                    .creatorNickname(creatorNickname)
                    .creatorProfileImageUrl(creatorProfile)
                    .text("Hello")
                    .recipientIds(recipients)
                    .build();

            assertThat(buzz.getRecipientIds()).hasSize(150);
        }

        @Test
        @DisplayName("본문이 비어있으면 생성에 실패한다")
        void createBuzz_Fail_BlankText() {
            assertThatThrownBy(() -> Buzz.builder()
                    .creatorId(creatorId).creatorNickname(creatorNickname).creatorProfileImageUrl(creatorProfile)
                    .text("  ").recipientIds(List.of(recipientId)).build())
                    .isInstanceOf(BuzzInvalidStateException.class);
        }

        @Test
        @DisplayName("본문이 1000자를 초과하면 생성에 실패한다")
        void createBuzz_Fail_TooLongText() {
            String tooLong = "가".repeat(Buzz.TEXT_MAX_LENGTH + 1);

            assertThatThrownBy(() -> Buzz.builder()
                    .creatorId(creatorId).creatorNickname(creatorNickname).creatorProfileImageUrl(creatorProfile)
                    .text(tooLong).recipientIds(List.of(recipientId)).build())
                    .isInstanceOf(BuzzInvalidStateException.class);
        }

        @Test
        @DisplayName("본문이 정확히 1000자이면 생성에 성공한다")
        void createBuzz_Success_MaxLengthText() {
            String boundary = "가".repeat(Buzz.TEXT_MAX_LENGTH);

            Buzz buzz = Buzz.builder()
                    .creatorId(creatorId).creatorNickname(creatorNickname).creatorProfileImageUrl(creatorProfile)
                    .text(boundary).recipientIds(List.of(recipientId)).build();

            assertThat(buzz.getText()).hasSize(Buzz.TEXT_MAX_LENGTH);
        }

        @Test
        @DisplayName("생성 시 필드들이 정상적으로 초기화된다")
        void createBuzz_Success() {
            Buzz buzz = Buzz.builder()
                    .creatorId(creatorId)
                    .creatorNickname(creatorNickname)
                    .creatorProfileImageUrl(creatorProfile)
                    .text("Hello")
                    .recipientIds(List.of(recipientId))
                    .build();

            assertThat(buzz.getCreatorNickname()).isEqualTo(creatorNickname);
            assertThat(buzz.getExpiresAt()).isAfter(buzz.getCreatedAt());
        }
    }

    @Nested
    @DisplayName("만료 검증 테스트")
    class ExpiryValidationTest {
        private Buzz buzz;

        @BeforeEach
        void setUp() {
            buzz = Buzz.builder()
                    .creatorId(creatorId)
                    .creatorNickname(creatorNickname)
                    .creatorProfileImageUrl(creatorProfile)
                    .text("Original Text")
                    .recipientIds(List.of(recipientId))
                    .build();
        }

        @Test
        @DisplayName("만료되지 않은 버즈는 통과한다")
        void validateNotExpired_Success() {
            // when & then
            buzz.validateNotExpired();
        }

        @Test
        @DisplayName("만료된 버즈는 예외가 발생한다")
        void validateNotExpired_Fail_Expired() {
            // given
            ReflectionTestUtils.setField(buzz, "expiresAt", buzz.getCreatedAt().minusMinutes(1));

            // when & then
            assertThatThrownBy(() -> buzz.validateNotExpired())
                    .isInstanceOf(BuzzInvalidStateException.class);
        }
    }

    @Nested
    @DisplayName("접근 및 삭제 권한 검증")
    class AccessValidationTest {
        private Buzz buzz;

        @BeforeEach
        void setUp() {
            buzz = Buzz.builder()
                    .creatorId(creatorId)
                    .creatorNickname(creatorNickname)
                    .creatorProfileImageUrl(creatorProfile)
                    .text("Test")
                    .recipientIds(List.of(recipientId))
                    .build();
        }

        @Test
        @DisplayName("수신자는 버즈에 접근할 수 있다")
        void validateAccess_Success_Recipient() {
            buzz.validateAccess(recipientId);
        }

        @Test
        @DisplayName("작성자는 버즈에 접근할 수 있다")
        void validateAccess_Success_Creator() {
            buzz.validateAccess(creatorId);
        }

        @Test
        @DisplayName("제3자가 접근하면 예외가 발생한다")
        void validateAccess_Fail_Stranger() {
            assertThatThrownBy(() -> buzz.validateAccess(strangerId))
                    .isInstanceOf(BuzzAccessDeniedException.class);
        }

        @Test
        @DisplayName("작성자는 버즈를 삭제할 수 있다")
        void validateDeletion_Success_Creator() {
            buzz.validateDeletion(creatorId);
        }

        @Test
        @DisplayName("작성자가 아닌 사람이 삭제하려 하면 예외가 발생한다")
        void validateDeletion_Fail_NonCreator() {
            assertThatThrownBy(() -> buzz.validateDeletion(recipientId))
                    .isInstanceOf(BuzzAccessDeniedException.class);
        }
    }

    @Nested
    @DisplayName("isCreator 테스트")
    class IsCreatorTest {
        private Buzz buzz;

        @BeforeEach
        void setUp() {
            buzz = Buzz.builder()
                    .creatorId(creatorId)
                    .creatorNickname(creatorNickname)
                    .creatorProfileImageUrl(creatorProfile)
                    .text("Test")
                    .recipientIds(List.of(recipientId))
                    .build();
        }

        @Test
        @DisplayName("creatorId와 같은 userId이면 isCreator가 true이다")
        void isCreator_WithCreatorId_ReturnsTrue() {
            assertThat(buzz.isCreator(creatorId)).isTrue();
        }

        @Test
        @DisplayName("creatorId와 다른 userId이면 isCreator가 false이다")
        void isCreator_WithOtherId_ReturnsFalse() {
            assertThat(buzz.isCreator(recipientId)).isFalse();
        }
    }
}
