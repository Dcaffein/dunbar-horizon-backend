package com.example.DunbarHorizon.buzz.adapter.out.mongo;

import com.example.DunbarHorizon.buzz.adapter.out.persistence.mongo.BuzzMongoTemplateRepository;
import com.example.DunbarHorizon.buzz.domain.Buzz;
import com.example.DunbarHorizon.buzz.domain.BuzzReply;
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

        // 기본 테스트 데이터(Buzz) 생성
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
    @DisplayName("답장 추가 (addReply) 테스트")
    class AddReplyTest {
        @Test
        @DisplayName("답장을 추가하면 replies 배열에 저장되고 작성자는 자동으로 읽음 처리된다")
        void addReply_Success() {
            // given
            BuzzReply reply = BuzzReply.of("res-1", recipientId, "답장자", "p.png", "답장 내용", null, true);

            // when
            templateRepository.addReply(savedBuzzId, reply);

            // then
            Buzz updated = mongoTemplate.findById(savedBuzzId, Buzz.class);
            assertThat(updated.getReplies()).hasSize(1);
            assertThat(updated.getReadRecipientIds()).contains(recipientId); // $addToSet 확인
        }
    }

    @Nested
    @DisplayName("답장 수정 (updateReply) 테스트")
    class UpdateReplyTest {
        @Test
        @DisplayName("포지셔널 연산자($)를 통해 특정 답장의 내용만 수정되어야 한다")
        void updateReply_Success() {
            // given
            BuzzReply reply1 = BuzzReply.of("res-1", recipientId, "닉1", "p1", "원본1", null, true);
            BuzzReply reply2 = BuzzReply.of("res-2", recipientId, "닉2", "p2", "원본2", null, true);
            templateRepository.addReply(savedBuzzId, reply1);
            templateRepository.addReply(savedBuzzId, reply2);

            // when
            String newText = "수정된 내용";
            templateRepository.updateReply(savedBuzzId, "res-1", newText, List.of("new_img.jpg"));

            // then
            Buzz updated = mongoTemplate.findById(savedBuzzId, Buzz.class);
            BuzzReply updatedReply = updated.getReplies().stream()
                    .filter(r -> r.getReplyId().equals("res-1"))
                    .findFirst().orElseThrow();

            assertThat(updatedReply.getText()).isEqualTo(newText);
            assertThat(updatedReply.getImageUrls()).containsExactly("new_img.jpg");

            // 다른 답장은 그대로여야 함
            BuzzReply otherReply = updated.getReplies().stream()
                    .filter(r -> r.getReplyId().equals("res-2"))
                    .findFirst().orElseThrow();
            assertThat(otherReply.getText()).isEqualTo("원본2");
        }
    }

    @Nested
    @DisplayName("답장 삭제 (removeReply) 테스트")
    class RemoveReplyTest {
        @Test
        @DisplayName("배열에서 특정 responseId를 가진 요소가 삭제되어야 한다")
        void removeReply_Success() {
            // given
            templateRepository.addReply(savedBuzzId, BuzzReply.of("res-1", recipientId, "닉", "p", "내용", null, true));

            // when
            templateRepository.removeReply(savedBuzzId, "res-1");

            // then
            Buzz updated = mongoTemplate.findById(savedBuzzId, Buzz.class);
            assertThat(updated.getReplies()).isEmpty();
        }
    }

    @Nested
    @DisplayName("안 읽은 발신자 조회 (findUnreadSenderIds) 테스트")
    class UnreadQueryTest {
        @Test
        @DisplayName("수신 여부, 읽음 여부, 차단 여부를 모두 만족하는 발신자 ID 목록을 가져온다")
        void findUnreadSenderIds_Success() {
            // given
            // 1. 아직 안 읽은 친구 버즈 (포함 대상 - 10L)
            saveBuzz(10L, List.of(recipientId));

            // 2. 이미 읽은 버즈 (제외 대상 - 11L)
            // 리포지토리에 markAsRead가 없으므로 mongoTemplate으로 직접 읽음 상태 주입
            Buzz readBuzz = saveBuzz(11L, List.of(recipientId));
            manuallyMarkAsRead(readBuzz.getId(), recipientId);

            // 3. 나를 차단한 친구 버즈 (제외 대상 - 12L)
            saveBuzz(12L, List.of(recipientId));

            // 4. 수신자가 내가 아닌 버즈 (제외 대상 - 13L)
            saveBuzz(13L, List.of(99L));

            Set<Long> blockedIds = Set.of(12L);

            // when
            List<Long> unreadSenderIds = templateRepository.findUnreadSenderIds(recipientId, blockedIds);

            // then
            // setUp에서 만든 1L과 방금 만든 10L만 포함되어야 함 (버즈 기준)
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