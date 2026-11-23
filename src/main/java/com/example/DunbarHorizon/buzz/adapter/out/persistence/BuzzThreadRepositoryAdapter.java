package com.example.DunbarHorizon.buzz.adapter.out.persistence;

import com.example.DunbarHorizon.buzz.adapter.out.persistence.jpa.BuzzThreadJpaRepository;
import com.example.DunbarHorizon.buzz.domain.BuzzThread;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzThreadAlreadyExistsException;
import com.example.DunbarHorizon.buzz.application.port.out.BuzzThreadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class BuzzThreadRepositoryAdapter implements BuzzThreadRepository {

    private static final String STARTER_VISIBILITY_UNIQUE_KEY = "uk_buzz_threads_buzz_starter_visibility";
    private static final String IDS_BY_BUZZ_IDS = "SELECT id FROM buzz_threads WHERE buzz_id IN (:buzzIds)";
    private static final String DELETE_BY_BUZZ_IDS = "DELETE FROM buzz_threads WHERE buzz_id IN (:buzzIds)";

    private final BuzzThreadJpaRepository buzzThreadJpaRepository;
    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public BuzzThread save(BuzzThread buzzThread) {
        try {
            return buzzThreadJpaRepository.saveAndFlush(buzzThread);
        } catch (DataIntegrityViolationException e) {
            if (isStarterVisibilityDuplicate(e)) {
                throw new BuzzThreadAlreadyExistsException(
                        buzzThread.getBuzzId(), buzzThread.getStarterId(), buzzThread.getVisibility());
            }
            throw e;
        }
    }

    @Override
    public Optional<BuzzThread> findById(Long threadId) {
        return buzzThreadJpaRepository.findById(threadId);
    }

    @Override
    public Optional<BuzzThread> findByIdForShare(Long threadId) {
        return buzzThreadJpaRepository.findByIdForShare(threadId);
    }

    @Override
    public void deleteById(Long threadId) {
        buzzThreadJpaRepository.deleteById(threadId);
    }

    @Override
    public List<Long> findIdsByBuzzIds(Collection<Long> buzzIds) {
        if (buzzIds.isEmpty()) {
            return List.of();
        }
        return jdbc.queryForList(IDS_BY_BUZZ_IDS, Map.of("buzzIds", buzzIds), Long.class);
    }

    @Override
    public void deleteAllByBuzzIds(Collection<Long> buzzIds) {
        if (buzzIds.isEmpty()) {
            return;
        }
        jdbc.update(DELETE_BY_BUZZ_IDS, Map.of("buzzIds", buzzIds));
    }

    private static boolean isStarterVisibilityDuplicate(DataIntegrityViolationException e) {
        String message = e.getMostSpecificCause().getMessage();
        return message != null && message.contains(STARTER_VISIBILITY_UNIQUE_KEY);
    }
}
