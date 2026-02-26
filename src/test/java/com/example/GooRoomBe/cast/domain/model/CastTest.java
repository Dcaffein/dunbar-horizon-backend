package com.example.GooRoomBe.cast.domain.model;

import com.example.GooRoomBe.cast.domain.exception.CastAccessDeniedException;
import com.example.GooRoomBe.cast.domain.exception.CastInvalidStateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CastTest {

    private final Long creatorId = 1L;
    private final String creatorNickname = "방장";
    private final String creatorProfile = "boss.png";

    private final Long recipientId = 2L;
    private final String recipientNickname = "수신자";
    private final String recipientProfile = "user.png";

    private final Long strangerId = 99L;

    @Nested
    @DisplayName("캐스트 생성 및 초기화 테스트")
    class CreationTest {
        @Test
        @DisplayName("수신자가 한 명도 없으면 생성에 실패한다")
        void createCast_Fail_NoRecipients() {
            assertThatThrownBy(() -> Cast.builder()
                    .creatorId(creatorId)
                    .creatorNickname(creatorNickname)
                    .creatorProfileImageUrl(creatorProfile)
                    .text("Hello")
                    .recipientIds(List.of())
                    .build())
                    .isInstanceOf(CastInvalidStateException.class);
        }

        @Test
        @DisplayName("생성 시 필드들이 정상적으로 초기화된다")
        void createCast_Success() {
            Cast cast = Cast.builder()
                    .creatorId(creatorId)
                    .creatorNickname(creatorNickname)
                    .creatorProfileImageUrl(creatorProfile)
                    .text("Hello")
                    .recipientIds(List.of(recipientId))
                    .build();

            assertThat(cast.getCreatorNickname()).isEqualTo(creatorNickname);
            assertThat(cast.getExpiresAt()).isAfter(cast.getCreatedAt());
        }
    }

    @Nested
    @DisplayName("답장(Response) 생성 비즈니스 로직 테스트")
    class ResponseCreationTest {
        private Cast cast;

        @BeforeEach
        void setUp() {
            cast = Cast.builder()
                    .creatorId(creatorId)
                    .creatorNickname(creatorNickname)
                    .creatorProfileImageUrl(creatorProfile)
                    .text("Test Cast")
                    .recipientIds(List.of(recipientId))
                    .build();
        }

        @Test
        @DisplayName("답장을 남기면 작성자의 정보를 저장")
        void createResponse_Success_WithProfile() {
            // when
            CastReply response = cast.createReply(
                    recipientId, recipientNickname, recipientProfile, "Good Reply", null, true);

            // then
            assertThat(response.getReplierNickname()).isEqualTo(recipientNickname);
            assertThat(response.getReplierProfileImageUrl()).isEqualTo(recipientProfile);
            assertThat(cast.getReadRecipientIds()).contains(recipientId);
        }
    }

    @Nested
    @DisplayName("수정 및 삭제 권한 테스트")
    class AuthorityTest {
        private Cast cast;
        private CastReply response;

        @BeforeEach
        void setUp() {
            cast = Cast.builder()
                    .creatorId(creatorId)
                    .creatorNickname(creatorNickname)
                    .creatorProfileImageUrl(creatorProfile)
                    .text("Original Text")
                    .recipientIds(List.of(recipientId))
                    .build();

            response = cast.createReply(recipientId, recipientNickname, recipientProfile, "Original Reply", null,true);

            List<CastReply> responses = new ArrayList<>();
            responses.add(response);

            ReflectionTestUtils.setField(cast, "responses", responses);
        }

        @Test
        @DisplayName("답장 작성자가 아닌 사람이 수정을 시도하면 예외가 발생한다")
        void updateResponse_Fail_NotAuthor() {
            assertThatThrownBy(() -> cast.updateReply(creatorId, response.getReplyId(), "New Text", null))
                    .isInstanceOf(CastAccessDeniedException.class);
        }

        @Test
        @DisplayName("답장 삭제는 작성자 본인이 할 수 있다")
        void validateResponseDeletion_Success_ByAuthor() {
            cast.validateReplyDeletion(recipientId, response.getReplyId());
        }

        @Test
        @DisplayName("답장 삭제는 캐스트 생성자도 할 수 있다")
        void validateResponseDeletion_Success_ByCreator() {
            cast.validateReplyDeletion(creatorId, response.getReplyId());
        }

        @Test
        @DisplayName("제3자가 답장 삭제를 시도하면 예외가 발생한다")
        void validateResponseDeletion_Fail_ByStranger() {
            assertThatThrownBy(() -> cast.validateReplyDeletion(strangerId, response.getReplyId()))
                    .isInstanceOf(CastAccessDeniedException.class);
        }
    }
}