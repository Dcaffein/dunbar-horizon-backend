package com.example.DunbarHorizon.buzz.adapter.out.persistence;

import com.example.DunbarHorizon.buzz.application.service.BuzzPurgeService;
import com.example.DunbarHorizon.buzz.domain.Buzz;
import com.example.DunbarHorizon.buzz.domain.BuzzAudienceMember;
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
@Import({BuzzRepositoryAdapter.class, BuzzAudienceMemberRepositoryAdapter.class, BuzzQueryRepositoryAdapter.class,
        BuzzThreadRepositoryAdapter.class, BuzzCommentRepositoryAdapter.class, BuzzPurgeService.class})
class BuzzPurgeIntegrationTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 22, 12, 0);

    @Autowired private BuzzPurgeService buzzPurgeService;
    @Autowired private BuzzRepositoryAdapter buzzRepositoryAdapter;
    @Autowired private BuzzAudienceMemberRepositoryAdapter buzzAudienceMemberRepositoryAdapter;
    @Autowired private BuzzThreadRepositoryAdapter buzzThreadRepositoryAdapter;
    @Autowired private BuzzCommentRepositoryAdapter buzzCommentRepositoryAdapter;
    @Autowired private EntityManager entityManager;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("생성 후 6시간이 지난 버즈와 삭제 표시된 버즈만 멤버·스레드·답글과 함께 지운다")
    void purgeExpiredAndDeletedBuzzes_RemovesExpiredAndDeletedWithChildren() {
        // given
        Buzz expired = saveBuzzWithThreadAndComment(NOW.minusHours(7));
        Buzz lifetimeEndsNow = saveBuzzWithThreadAndComment(NOW.minusHours(6));
        Buzz deleted = saveBuzzWithThreadAndComment(NOW.minusHours(1));
        Buzz live = saveBuzzWithThreadAndComment(NOW.minusHours(6).plusSeconds(1));
        entityManager.flush();
        jdbcTemplate.update("UPDATE buzzes SET deleted_at = ? WHERE id = ?", NOW.minusMinutes(30), deleted.getId());
        entityManager.clear();

        // when
        int purged = buzzPurgeService.purgeExpiredAndDeletedBuzzes(NOW);

        // then
        assertThat(purged).isEqualTo(3);
        for (Buzz removed : List.of(expired, lifetimeEndsNow, deleted)) {
            assertThat(count("SELECT COUNT(*) FROM buzzes WHERE id = ?", removed.getId())).isZero();
            assertThat(count("SELECT COUNT(*) FROM buzz_audience_members WHERE buzz_id = ?", removed.getId())).isZero();
            assertThat(count("SELECT COUNT(*) FROM buzz_threads WHERE buzz_id = ?", removed.getId())).isZero();
        }
        assertThat(count("SELECT COUNT(*) FROM buzz_audience_members WHERE buzz_id = ?", live.getId())).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM buzz_threads WHERE buzz_id = ?", live.getId())).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM buzz_comments c JOIN buzz_threads t ON t.id = c.thread_id "
                + "WHERE t.buzz_id = ?", live.getId())).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM buzz_comments WHERE thread_id NOT IN (SELECT id FROM buzz_threads)",
                null)).isZero();
    }

    private Buzz saveBuzzWithThreadAndComment(LocalDateTime createdAt) {
        Buzz buzz = Buzz.create(1L, "본문", List.of(), List.of(2L), createdAt);
        buzzRepositoryAdapter.save(buzz);
        buzzAudienceMemberRepositoryAdapter.saveAll(List.of(BuzzAudienceMember.create(buzz.getId(), 2L)));
        BuzzThread thread = buzzThreadRepositoryAdapter.save(
                BuzzThread.create(buzz, 2L, true, ThreadVisibility.PUBLIC, "첫 글", List.of(), createdAt));
        buzzCommentRepositoryAdapter.save(BuzzComment.write(thread, 1L, "답글", List.of(), createdAt));
        return buzz;
    }

    private long count(String sql, Long buzzId) {
        Long count = buzzId == null
                ? jdbcTemplate.queryForObject(sql, Long.class)
                : jdbcTemplate.queryForObject(sql, Long.class, buzzId);
        return count == null ? 0 : count;
    }
}
