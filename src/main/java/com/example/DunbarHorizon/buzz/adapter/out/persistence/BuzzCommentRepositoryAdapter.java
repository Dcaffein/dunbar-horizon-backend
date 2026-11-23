package com.example.DunbarHorizon.buzz.adapter.out.persistence;

import com.example.DunbarHorizon.buzz.adapter.out.persistence.jpa.BuzzCommentJpaRepository;
import com.example.DunbarHorizon.buzz.domain.BuzzComment;
import com.example.DunbarHorizon.buzz.application.port.out.BuzzCommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class BuzzCommentRepositoryAdapter implements BuzzCommentRepository {

    private static final String DELETE_BY_THREAD_IDS = "DELETE FROM buzz_comments WHERE thread_id IN (:threadIds)";

    private final BuzzCommentJpaRepository buzzCommentJpaRepository;
    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public BuzzComment save(BuzzComment buzzComment) {
        return buzzCommentJpaRepository.save(buzzComment);
    }

    @Override
    public Optional<BuzzComment> findById(Long commentId) {
        return buzzCommentJpaRepository.findById(commentId);
    }

    @Override
    public void deleteById(Long commentId) {
        buzzCommentJpaRepository.deleteById(commentId);
    }

    @Override
    public void deleteAllByThreadId(Long threadId) {
        buzzCommentJpaRepository.deleteAllByThreadId(threadId);
    }

    @Override
    public void deleteAllByThreadIds(Collection<Long> threadIds) {
        if (threadIds.isEmpty()) {
            return;
        }
        jdbc.update(DELETE_BY_THREAD_IDS, Map.of("threadIds", threadIds));
    }
}
