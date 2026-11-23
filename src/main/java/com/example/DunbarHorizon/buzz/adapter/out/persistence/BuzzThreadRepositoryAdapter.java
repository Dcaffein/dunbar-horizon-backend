package com.example.DunbarHorizon.buzz.adapter.out.persistence;

import com.example.DunbarHorizon.buzz.adapter.out.persistence.jpa.BuzzThreadJpaRepository;
import com.example.DunbarHorizon.buzz.domain.BuzzThread;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzThreadAlreadyExistsException;
import com.example.DunbarHorizon.buzz.application.port.out.BuzzThreadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

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



    private static boolean isStarterVisibilityDuplicate(DataIntegrityViolationException e) {
        String message = e.getMostSpecificCause().getMessage();
        return message != null && message.contains(STARTER_VISIBILITY_UNIQUE_KEY);
    }
}
