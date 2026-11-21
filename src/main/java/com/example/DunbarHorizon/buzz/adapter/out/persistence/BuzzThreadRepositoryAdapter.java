package com.example.DunbarHorizon.buzz.adapter.out.persistence;

import com.example.DunbarHorizon.buzz.adapter.out.persistence.jpa.BuzzThreadJpaRepository;
import com.example.DunbarHorizon.buzz.domain.BuzzThread;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzThreadAlreadyOpenedException;
import com.example.DunbarHorizon.buzz.domain.repository.BuzzThreadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class BuzzThreadRepositoryAdapter implements BuzzThreadRepository {

    private static final String STARTER_VISIBILITY_UNIQUE_KEY = "uk_buzz_threads_buzz_starter_visibility";

    private final BuzzThreadJpaRepository buzzThreadJpaRepository;

    @Override
    public BuzzThread save(BuzzThread buzzThread) {
        try {
            return buzzThreadJpaRepository.saveAndFlush(buzzThread);
        } catch (DataIntegrityViolationException e) {
            if (isStarterVisibilityDuplicate(e)) {
                throw new BuzzThreadAlreadyOpenedException(
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
    public List<BuzzThread> findAllByBuzzId(String buzzId) {
        return buzzThreadJpaRepository.findAllByBuzzIdOrderByCreatedAtAscIdAsc(buzzId);
    }

    @Override
    public void deleteAllByBuzzId(String buzzId) {
        buzzThreadJpaRepository.deleteAllByBuzzId(buzzId);
    }

    private static boolean isStarterVisibilityDuplicate(DataIntegrityViolationException e) {
        String message = e.getMostSpecificCause().getMessage();
        return message != null && message.contains(STARTER_VISIBILITY_UNIQUE_KEY);
    }
}
