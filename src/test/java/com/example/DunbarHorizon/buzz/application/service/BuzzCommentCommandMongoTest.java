package com.example.DunbarHorizon.buzz.application.service;

import com.example.DunbarHorizon.buzz.adapter.out.persistence.BuzzRepositoryAdapter;
import com.example.DunbarHorizon.buzz.adapter.out.persistence.mongo.BuzzMongoTemplateRepository;
import com.example.DunbarHorizon.buzz.application.port.out.BuzzSocialPort;
import com.example.DunbarHorizon.buzz.application.port.out.ImageStoragePort;
import com.example.DunbarHorizon.buzz.domain.Buzz;
import com.example.DunbarHorizon.buzz.domain.BuzzComment;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzAccessDeniedException;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzCommentNotFoundException;
import com.example.DunbarHorizon.buzz.domain.repository.BuzzRepository;
import com.example.DunbarHorizon.support.MongoRepositoryTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@MongoRepositoryTest
@Import({BuzzRepositoryAdapter.class, BuzzMongoTemplateRepository.class})
@DisplayName("댓글이 상세 조회 slice 크기를 넘는 버즈의 댓글 명령")
class BuzzCommentCommandMongoTest {

    private static final int COMMENT_COUNT = 25;
    private static final String OLDEST_COMMENT_ID = "com-1";

    @Autowired
    private BuzzRepository buzzRepository;

    @Autowired
    private BuzzMongoTemplateRepository templateRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    private BuzzService buzzService;
    private ImageStoragePort imageStoragePort;

    private final Long creatorId = 1L;
    private final Long commenterId = 2L;
    private final Long otherRecipientId = 3L;

    private String buzzId;

    @BeforeEach
    void setUp() {
        imageStoragePort = mock(ImageStoragePort.class);
        buzzService = new BuzzService(
                mock(RecipientStrategyProvider.class),
                buzzRepository,
                mock(BuzzSocialPort.class),
                imageStoragePort,
                mock(ApplicationEventPublisher.class));

        mongoTemplate.dropCollection(Buzz.class);

        Buzz buzz = Buzz.builder()
                .creatorId(creatorId)
                .creatorNickname("작성자")
                .creatorProfileImageUrl("p.png")
                .text("본문")
                .recipientIds(List.of(commenterId, otherRecipientId))
                .build();
        buzzId = mongoTemplate.save(buzz).getId();

        IntStream.rangeClosed(1, COMMENT_COUNT).forEach(i ->
                templateRepository.addComment(buzzId,
                        BuzzComment.of("com-" + i, commenterId, "댓글자", "p.png", "내용" + i, List.of(), true)));
    }

    @Test
    @DisplayName("가장 오래된 댓글을 그 작성자가 수정할 수 있다")
    void updateComment_Oldest_Succeeds() {
        // given
        given(imageStoragePort.resolveUrls(anyList())).willReturn(List.of());

        // when
        buzzService.updateComment(commenterId, buzzId, OLDEST_COMMENT_ID, "수정된 내용", List.of());

        // then
        assertThat(findComment(OLDEST_COMMENT_ID).getText()).isEqualTo("수정된 내용");
    }

    @Test
    @DisplayName("가장 오래된 댓글을 그 작성자가 삭제할 수 있다")
    void deleteComment_Oldest_ByAuthor_Succeeds() {
        // when
        buzzService.deleteComment(commenterId, buzzId, OLDEST_COMMENT_ID);

        // then
        assertThat(savedComments())
                .hasSize(COMMENT_COUNT - 1)
                .noneMatch(c -> c.getCommentId().equals(OLDEST_COMMENT_ID));
    }

    @Test
    @DisplayName("가장 오래된 댓글을 버즈 생성자가 삭제할 수 있다")
    void deleteComment_Oldest_ByBuzzCreator_Succeeds() {
        // when
        buzzService.deleteComment(creatorId, buzzId, OLDEST_COMMENT_ID);

        // then
        assertThat(savedComments())
                .hasSize(COMMENT_COUNT - 1)
                .noneMatch(c -> c.getCommentId().equals(OLDEST_COMMENT_ID));
    }

    @Test
    @DisplayName("작성자가 아닌 수신자가 가장 오래된 댓글을 수정하면 예외가 발생한다")
    void updateComment_Oldest_ByOtherRecipient_Throws() {
        // when & then
        assertThatThrownBy(() ->
                buzzService.updateComment(otherRecipientId, buzzId, OLDEST_COMMENT_ID, "수정 시도", List.of()))
                .isInstanceOf(BuzzAccessDeniedException.class);

        assertThat(findComment(OLDEST_COMMENT_ID).getText()).isEqualTo("내용1");
    }

    @Test
    @DisplayName("없는 댓글을 수정하면 예외가 발생한다")
    void updateComment_Missing_Throws() {
        // when & then
        assertThatThrownBy(() ->
                buzzService.updateComment(commenterId, buzzId, "com-999", "수정 시도", List.of()))
                .isInstanceOf(BuzzCommentNotFoundException.class);
    }

    private List<BuzzComment> savedComments() {
        return mongoTemplate.findById(buzzId, Buzz.class).getComments();
    }

    private BuzzComment findComment(String commentId) {
        return savedComments().stream()
                .filter(c -> c.getCommentId().equals(commentId))
                .findFirst()
                .orElseThrow();
    }
}
