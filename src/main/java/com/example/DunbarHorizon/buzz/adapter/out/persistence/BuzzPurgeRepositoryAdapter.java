package com.example.DunbarHorizon.buzz.adapter.out.persistence;

import com.example.DunbarHorizon.buzz.application.port.out.BuzzPurgeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class BuzzPurgeRepositoryAdapter implements BuzzPurgeRepository {

    private static final String PURGEABLE_IDS = """
            (SELECT id FROM buzzes WHERE created_at <= :createdAtOrBefore LIMIT :limit)
            UNION
            (SELECT id FROM buzzes WHERE deleted_at IS NOT NULL LIMIT :limit)
            LIMIT :limit
            """;

    private static final String DELETE_BUZZES = "DELETE FROM buzzes WHERE id IN (:buzzIds)";
    private static final String THREAD_IDS = "SELECT id FROM buzz_threads WHERE buzz_id IN (:buzzIds)";
    private static final String DELETE_AUDIENCE_MEMBERS = "DELETE FROM buzz_audience_members WHERE buzz_id IN (:buzzIds)";
    private static final String DELETE_THREADS = "DELETE FROM buzz_threads WHERE buzz_id IN (:buzzIds)";
    private static final String DELETE_COMMENTS = "DELETE FROM buzz_comments WHERE thread_id IN (:threadIds)";

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public List<Long> findPurgeableIds(LocalDateTime createdAtOrBefore, int limit) {
        return jdbc.queryForList(PURGEABLE_IDS, Map.of("createdAtOrBefore", createdAtOrBefore, "limit", limit), Long.class);
    }

    @Override
    public void purge(Collection<Long> buzzIds) {
        if (buzzIds.isEmpty()) {
            return;
        }
        Map<String, Object> byBuzzIds = Map.of("buzzIds", buzzIds);
        jdbc.update(DELETE_BUZZES, byBuzzIds);
        List<Long> threadIds = jdbc.queryForList(THREAD_IDS, byBuzzIds, Long.class);
        jdbc.update(DELETE_AUDIENCE_MEMBERS, byBuzzIds);
        jdbc.update(DELETE_THREADS, byBuzzIds);
        if (!threadIds.isEmpty()) {
            jdbc.update(DELETE_COMMENTS, Map.of("threadIds", threadIds));
        }
    }
}
