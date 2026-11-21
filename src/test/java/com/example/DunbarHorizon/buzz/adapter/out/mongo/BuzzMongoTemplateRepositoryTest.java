package com.example.DunbarHorizon.buzz.adapter.out.mongo;

import com.example.DunbarHorizon.buzz.adapter.out.persistence.mongo.BuzzMongoTemplateRepository;
import com.example.DunbarHorizon.buzz.domain.Buzz;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.DunbarHorizon.support.TestContainerConfig;
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
@Import({BuzzMongoTemplateRepository.class, TestContainerConfig.class})
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
