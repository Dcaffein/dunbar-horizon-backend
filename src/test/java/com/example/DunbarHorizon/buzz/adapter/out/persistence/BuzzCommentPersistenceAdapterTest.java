package com.example.DunbarHorizon.buzz.adapter.out.persistence;

import com.example.DunbarHorizon.buzz.domain.BuzzComment;
import com.example.DunbarHorizon.buzz.domain.BuzzThread;
import com.example.DunbarHorizon.buzz.domain.ThreadVisibility;
import com.example.DunbarHorizon.support.JpaRepositoryTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@JpaRepositoryTest
@Import({BuzzThreadRepositoryAdapter.class, BuzzCommentRepositoryAdapter.class})
class BuzzCommentPersistenceAdapterTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 22, 12, 0);

    @Autowired private BuzzThreadRepositoryAdapter buzzThreadRepositoryAdapter;
    @Autowired private BuzzCommentRepositoryAdapter buzzCommentRepositoryAdapter;
    @Autowired private EntityManager entityManager;
    @Autowired private JdbcTemplate jdbcTemplate;

    private BuzzThread thread(String buzzId, Long starterId) {
        return buzzThreadRepositoryAdapter.save(
                BuzzThread.open(buzzId, starterId, ThreadVisibility.PUBLIC, "첫 글", List.of(), NOW));
    }

    private BuzzComment comment(Long threadId, Long authorId, String text) {
        return buzzCommentRepositoryAdapter.save(BuzzComment.write(threadId, authorId, text, List.of("buzz/a"), NOW));
    }

    @Test
    @DisplayName("스레드의 답글은 쓴 순서대로 조회되고 이미지는 키로 저장된다")
    void findAllByThreadId_OrderedAndStoresKeys() {
        // given
        BuzzThread thread = thread("buzz-1", 2L);
        comment(thread.getId(), 3L, "첫 답글");
        comment(thread.getId(), 1L, "둘째 답글");
        entityManager.flush();
        entityManager.clear();

        // when
        List<BuzzComment> comments = buzzCommentRepositoryAdapter.findAllByThreadId(thread.getId());

        // then
        assertThat(comments).extracting(BuzzComment::getText).containsExactly("첫 답글", "둘째 답글");
        assertThat(jdbcTemplate.queryForList(
                "SELECT CAST(image_keys AS CHAR) FROM buzz_comments WHERE thread_id = ?", String.class, thread.getId()))
                .allSatisfy(keys -> assertThat(keys).contains("buzz/a").doesNotContain("X-Amz-"));
    }

    @Test
    @DisplayName("버즈 단위로 답글을 지우면 그 버즈 스레드의 답글만 지운다")
    void deleteAllByBuzzId_OnlyThatBuzz() {
        // given
        BuzzThread target = thread("buzz-1", 2L);
        BuzzThread other = thread("buzz-2", 2L);
        comment(target.getId(), 3L, "지울 답글");
        comment(other.getId(), 3L, "남을 답글");

        // when
        buzzCommentRepositoryAdapter.deleteAllByBuzzId("buzz-1");

        // then
        assertThat(buzzCommentRepositoryAdapter.findAllByThreadId(target.getId())).isEmpty();
        assertThat(buzzCommentRepositoryAdapter.findAllByThreadId(other.getId())).hasSize(1);
    }

    @Test
    @DisplayName("스레드 단위로 답글을 지우거나 답글 하나를 지운다")
    void deleteByThreadAndById() {
        // given
        BuzzThread thread = thread("buzz-1", 2L);
        BuzzComment kept = comment(thread.getId(), 3L, "남을 답글");
        BuzzComment removed = comment(thread.getId(), 3L, "지울 답글");

        // when
        buzzCommentRepositoryAdapter.deleteById(removed.getId());

        // then
        assertThat(buzzCommentRepositoryAdapter.findAllByThreadId(thread.getId()))
                .extracting(BuzzComment::getId).containsExactly(kept.getId());

        // when
        buzzCommentRepositoryAdapter.deleteAllByThreadId(thread.getId());

        // then
        assertThat(buzzCommentRepositoryAdapter.findAllByThreadId(thread.getId())).isEmpty();
    }
}
