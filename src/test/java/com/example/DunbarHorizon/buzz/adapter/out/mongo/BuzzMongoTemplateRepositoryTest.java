package com.example.DunbarHorizon.buzz.adapter.out.mongo;

import com.example.DunbarHorizon.buzz.adapter.out.persistence.mongo.BuzzMongoTemplateRepository;
import com.example.DunbarHorizon.buzz.domain.Buzz;
import com.example.DunbarHorizon.buzz.domain.BuzzComment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest
@Import(BuzzMongoTemplateRepository.class)
class BuzzMongoTemplateRepositoryTest {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private BuzzMongoTemplateRepository templateRepository;

    private String savedBuzzId;
    private final Long creatorId = 1L;
    private final Long recipientId = 2L;

    @BeforeEach
    void setUp() {
        mongoTemplate.dropCollection(Buzz.class);

        Buzz buzz = Buzz.builder()
                .creatorId(creatorId)
                .creatorNickname("작성자")
                .creatorProfileImageUrl("img.png")
                .text("본문 내용")
                .recipientIds(List.of(recipientId, 3L))
                .build();

        Buzz saved = mongoTemplate.save(buzz);
        savedBuzzId = saved.getId();
    }

    @Nested
    @DisplayName("댓글 추가 (addComment) 테스트")
    class AddCommentTest {
        @Test
        @DisplayName("댓글을 추가하면 comments 배열에 저장되고 작성자는 자동으로 읽음 처리된다")
        void addComment_Success() {
            // given
            BuzzComment comment = BuzzComment.of("com-1", recipientId, "댓글자", "p.png", "댓글 내용", null, true);

            // when
            templateRepository.addComment(savedBuzzId, comment);

            // then
            Buzz updated = mongoTemplate.findById(savedBuzzId, Buzz.class);
            assertThat(updated.getComments()).hasSize(1);
            assertThat(updated.getReadRecipientIds()).contains(recipientId);
        }
    }

    @Nested
    @DisplayName("댓글 수정 (updateComment) 테스트")
    class UpdateCommentTest {
        @Test
        @DisplayName("포지셔널 연산자($)를 통해 특정 댓글의 내용만 수정되어야 한다")
        void updateComment_Success() {
            // given
            BuzzComment comment1 = BuzzComment.of("com-1", recipientId, "닉1", "p1", "원본1", null, true);
            BuzzComment comment2 = BuzzComment.of("com-2", recipientId, "닉2", "p2", "원본2", null, true);
            templateRepository.addComment(savedBuzzId, comment1);
            templateRepository.addComment(savedBuzzId, comment2);

            // when
            String newText = "수정된 내용";
            templateRepository.updateComment(savedBuzzId, "com-1", newText, List.of("new_img.jpg"));

            // then
            Buzz updated = mongoTemplate.findById(savedBuzzId, Buzz.class);
            BuzzComment updatedComment = updated.getComments().stream()
                    .filter(c -> c.getCommentId().equals("com-1"))
                    .findFirst().orElseThrow();

            assertThat(updatedComment.getText()).isEqualTo(newText);
            assertThat(updatedComment.getImageUrls()).containsExactly("new_img.jpg");

            BuzzComment otherComment = updated.getComments().stream()
                    .filter(c -> c.getCommentId().equals("com-2"))
                    .findFirst().orElseThrow();
            assertThat(otherComment.getText()).isEqualTo("원본2");
        }
    }

    @Nested
    @DisplayName("댓글 삭제 (removeComment) 테스트")
    class RemoveCommentTest {
        @Test
        @DisplayName("배열에서 특정 commentId를 가진 요소가 삭제되어야 한다")
        void removeComment_Success() {
            // given
            templateRepository.addComment(savedBuzzId, BuzzComment.of("com-1", recipientId, "닉", "p", "내용", null, true));

            // when
            templateRepository.removeComment(savedBuzzId, "com-1");

            // then
            Buzz updated = mongoTemplate.findById(savedBuzzId, Buzz.class);
            assertThat(updated.getComments()).isEmpty();
        }
    }

    @Nested
    @DisplayName("댓글 슬라이스 조회 (findByIdWithCommentSlice) 테스트")
    class CommentSliceQueryTest {

        @Test
        @DisplayName("comments가 25개이면 최신 20개만 반환한다")
        void findByIdWithCommentSlice_Returns20WhenOver() {
            // given
            IntStream.rangeClosed(1, 25).forEach(i ->
                    templateRepository.addComment(savedBuzzId,
                            BuzzComment.of("com-" + i, recipientId, "닉", "p", "내용" + i, null, true)));

            // when
            Buzz result = templateRepository.findByIdWithCommentSlice(savedBuzzId);

            // then
            assertThat(result.getComments()).hasSize(20);
        }

        @Test
        @DisplayName("comments가 10개이면 전체 10개를 반환한다")
        void findByIdWithCommentSlice_ReturnsAllWhenUnder() {
            // given
            IntStream.rangeClosed(1, 10).forEach(i ->
                    templateRepository.addComment(savedBuzzId,
                            BuzzComment.of("com-" + i, recipientId, "닉", "p", "내용" + i, null, true)));

            // when
            Buzz result = templateRepository.findByIdWithCommentSlice(savedBuzzId);

            // then
            assertThat(result.getComments()).hasSize(10);
        }
    }

    @Nested
    @DisplayName("안 읽은 발신자 조회 (findUnreadSenderIds) 테스트")
    class UnreadQueryTest {
        @Test
        @DisplayName("수신 여부, 읽음 여부, 차단 여부를 모두 만족하는 발신자 ID 목록을 가져온다")
        void findUnreadSenderIds_Success() {
            // given
            saveBuzz(10L, List.of(recipientId));

            Buzz readBuzz = saveBuzz(11L, List.of(recipientId));
            manuallyMarkAsRead(readBuzz.getId(), recipientId);

            saveBuzz(12L, List.of(recipientId));

            saveBuzz(13L, List.of(99L));

            Set<Long> blockedIds = Set.of(12L);

            // when
            List<Long> unreadSenderIds = templateRepository.findUnreadSenderIds(recipientId, blockedIds);

            // then
            assertThat(unreadSenderIds).containsExactlyInAnyOrder(1L, 10L);
            assertThat(unreadSenderIds).doesNotContain(11L, 12L, 13L);
        }

        private Buzz saveBuzz(Long creatorId, List<Long> recipientIds) {
            return mongoTemplate.save(Buzz.builder()
                    .creatorId(creatorId).creatorNickname("N").creatorProfileImageUrl("P")
                    .text("T").recipientIds(recipientIds).build());
        }

        private void manuallyMarkAsRead(String buzzId, Long userId) {
            Query query = new Query(Criteria.where("id").is(buzzId));
            Update update = new Update().addToSet("readRecipientIds", userId);
            mongoTemplate.updateFirst(query, update, Buzz.class);
        }
    }
}
