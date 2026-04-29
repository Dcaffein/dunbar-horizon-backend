package com.example.DunbarHorizon.buzz.domain.model;

import com.example.DunbarHorizon.buzz.domain.Buzz;
import com.example.DunbarHorizon.buzz.domain.BuzzReply;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzAccessDeniedException;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzInvalidStateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BuzzTest {

    private final Long creatorId = 1L;
    private final String creatorNickname = "방장";
    private final String creatorProfile = "boss.png";

    private final Long recipientId = 2L;
    private final String recipientNickname = "수신자";
    private final String recipientProfile = "user.png";

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
    @DisplayName("답장(Response) 생성 비즈니스 로직 테스트")
    class ResponseCreationTest {
        private Buzz buzz;

        @BeforeEach
        void setUp() {
            buzz = Buzz.builder()
                    .creatorId(creatorId)
                    .creatorNickname(creatorNickname)
                    .creatorProfileImageUrl(creatorProfile)
                    .text("Test Buzz")
                    .recipientIds(List.of(recipientId))
                    .build();
        }

        @Test
        @DisplayName("수신자가 답장을 남기면 readRecipientIds에 추가된다")
        void createResponse_Success_WithProfile() {
            // when
            BuzzReply response = buzz.createReply(
                    recipientId, recipientNickname, recipientProfile, "Good Reply", null, true);

            // then
            assertThat(response.getReplierNickname()).isEqualTo(recipientNickname);
            assertThat(response.getReplierProfileImageUrl()).isEqualTo(recipientProfile);
            assertThat(buzz.getReadRecipientIds()).contains(recipientId);
        }

        @Test
        @DisplayName("작성자 본인이 답장을 남길 수 있다")
        void createReply_Success_ByCreator() {
            // when
            BuzzReply reply = buzz.createReply(
                    creatorId, creatorNickname, creatorProfile, "Creator Reply", null, true);

            // then
            assertThat(reply.getReplierId()).isEqualTo(creatorId);
        }

        @Test
        @DisplayName("작성자가 답장을 남겨도 readRecipientIds에 추가되지 않는다")
        void createReply_Creator_NotMarkedAsRead() {
            // when
            buzz.createReply(creatorId, creatorNickname, creatorProfile, "Creator Reply", null, true);

            // then
            assertThat(buzz.getReadRecipientIds()).doesNotContain(creatorId);
        }

        @Test
        @DisplayName("수신자도 작성자도 아닌 제3자가 답장을 시도하면 예외가 발생한다")
        void createReply_Fail_ByStranger() {
            // when & then
            assertThatThrownBy(() -> buzz.createReply(
                    strangerId, "낯선자", "stranger.png", "Stranger Reply", null, true))
                    .isInstanceOf(BuzzAccessDeniedException.class);
        }
    }

    @Nested
    @DisplayName("수정 및 삭제 권한 테스트")
    class AuthorityTest {
        private Buzz buzz;
        private BuzzReply response;

        @BeforeEach
        void setUp() {
            buzz = Buzz.builder()
                    .creatorId(creatorId)
                    .creatorNickname(creatorNickname)
                    .creatorProfileImageUrl(creatorProfile)
                    .text("Original Text")
                    .recipientIds(List.of(recipientId))
                    .build();

            response = buzz.createReply(recipientId, recipientNickname, recipientProfile, "Original Reply", null,true);

            List<BuzzReply> responses = new ArrayList<>();
            responses.add(response);

            ReflectionTestUtils.setField(buzz, "replies", responses);
        }

        @Test
        @DisplayName("답장 작성자가 아닌 사람이 수정을 시도하면 예외가 발생한다")
        void updateResponse_Fail_NotAuthor() {
            assertThatThrownBy(() -> buzz.updateReply(creatorId, response.getReplyId(), "New Text", null))
                    .isInstanceOf(BuzzAccessDeniedException.class);
        }

        @Test
        @DisplayName("답장 삭제는 작성자 본인이 할 수 있다")
        void validateResponseDeletion_Success_ByAuthor() {
            buzz.validateReplyDeletion(recipientId, response.getReplyId());
        }

        @Test
        @DisplayName("답장 삭제는 버즈 생성자도 할 수 있다")
        void validateResponseDeletion_Success_ByCreator() {
            buzz.validateReplyDeletion(creatorId, response.getReplyId());
        }

        @Test
        @DisplayName("제3자가 답장 삭제를 시도하면 예외가 발생한다")
        void validateResponseDeletion_Fail_ByStranger() {
            assertThatThrownBy(() -> buzz.validateReplyDeletion(strangerId, response.getReplyId()))
                    .isInstanceOf(BuzzAccessDeniedException.class);
        }
    }
}