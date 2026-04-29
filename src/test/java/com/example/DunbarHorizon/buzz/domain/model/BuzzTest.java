package com.example.DunbarHorizon.buzz.domain.model;

import com.example.DunbarHorizon.buzz.domain.Buzz;
import com.example.DunbarHorizon.buzz.domain.BuzzComment;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzAccessDeniedException;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzInvalidStateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.LongStream;

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
    @DisplayName("댓글(Comment) 생성 비즈니스 로직 테스트")
    class CommentCreationTest {
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
        @DisplayName("수신자가 댓글을 남기면 readRecipientIds에 추가된다")
        void createComment_Success_WithProfile() {
            // when
            BuzzComment comment = buzz.createComment(
                    recipientId, recipientNickname, recipientProfile, "Good Comment", null, true);

            // then
            assertThat(comment.getCommenterNickname()).isEqualTo(recipientNickname);
            assertThat(comment.getCommenterProfileImageUrl()).isEqualTo(recipientProfile);
            assertThat(buzz.getReadRecipientIds()).contains(recipientId);
        }

        @Test
        @DisplayName("작성자 본인이 댓글을 남길 수 있다")
        void createComment_Success_ByCreator() {
            // when
            BuzzComment comment = buzz.createComment(
                    creatorId, creatorNickname, creatorProfile, "Creator Comment", null, true);

            // then
            assertThat(comment.getCommenterId()).isEqualTo(creatorId);
        }

        @Test
        @DisplayName("작성자가 댓글을 남겨도 readRecipientIds에 추가되지 않는다")
        void createComment_Creator_NotMarkedAsRead() {
            // when
            buzz.createComment(creatorId, creatorNickname, creatorProfile, "Creator Comment", null, true);

            // then
            assertThat(buzz.getReadRecipientIds()).doesNotContain(creatorId);
        }

        @Test
        @DisplayName("수신자도 작성자도 아닌 제3자가 댓글을 시도하면 예외가 발생한다")
        void createComment_Fail_ByStranger() {
            // when & then
            assertThatThrownBy(() -> buzz.createComment(
                    strangerId, "낯선자", "stranger.png", "Stranger Comment", null, true))
                    .isInstanceOf(BuzzAccessDeniedException.class);
        }
    }

    @Nested
    @DisplayName("수정 및 삭제 권한 테스트")
    class AuthorityTest {
        private Buzz buzz;
        private BuzzComment comment;

        @BeforeEach
        void setUp() {
            buzz = Buzz.builder()
                    .creatorId(creatorId)
                    .creatorNickname(creatorNickname)
                    .creatorProfileImageUrl(creatorProfile)
                    .text("Original Text")
                    .recipientIds(List.of(recipientId))
                    .build();

            comment = buzz.createComment(recipientId, recipientNickname, recipientProfile, "Original Comment", null, true);

            List<BuzzComment> comments = new ArrayList<>();
            comments.add(comment);

            ReflectionTestUtils.setField(buzz, "comments", comments);
        }

        @Test
        @DisplayName("댓글 작성자가 아닌 사람이 수정을 시도하면 예외가 발생한다")
        void updateComment_Fail_NotAuthor() {
            assertThatThrownBy(() -> buzz.updateComment(creatorId, comment.getCommentId(), "New Text", null))
                    .isInstanceOf(BuzzAccessDeniedException.class);
        }

        @Test
        @DisplayName("댓글 삭제는 작성자 본인이 할 수 있다")
        void validateCommentDeletion_Success_ByAuthor() {
            buzz.validateCommentDeletion(recipientId, comment.getCommentId());
        }

        @Test
        @DisplayName("댓글 삭제는 버즈 생성자도 할 수 있다")
        void validateCommentDeletion_Success_ByCreator() {
            buzz.validateCommentDeletion(creatorId, comment.getCommentId());
        }

        @Test
        @DisplayName("제3자가 댓글 삭제를 시도하면 예외가 발생한다")
        void validateCommentDeletion_Fail_ByStranger() {
            assertThatThrownBy(() -> buzz.validateCommentDeletion(strangerId, comment.getCommentId()))
                    .isInstanceOf(BuzzAccessDeniedException.class);
        }
    }
}
