package com.example.DunbarHorizon.buzz.adapter.out.persistence;

import com.example.DunbarHorizon.buzz.adapter.out.persistence.jpa.BuzzCommentJpaRepository;
import com.example.DunbarHorizon.buzz.domain.BuzzComment;
import com.example.DunbarHorizon.buzz.domain.repository.BuzzCommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class BuzzCommentRepositoryAdapter implements BuzzCommentRepository {

    private final BuzzCommentJpaRepository buzzCommentJpaRepository;

    @Override
    public BuzzComment save(BuzzComment buzzComment) {
        return buzzCommentJpaRepository.save(buzzComment);
    }

    @Override
    public Optional<BuzzComment> findById(Long commentId) {
        return buzzCommentJpaRepository.findById(commentId);
    }

    @Override
    public List<BuzzComment> findAllByThreadId(Long threadId) {
        return buzzCommentJpaRepository.findAllByThreadIdOrderByIdAsc(threadId);
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
        buzzCommentJpaRepository.deleteAllByThreadIdIn(threadIds);
    }
}
