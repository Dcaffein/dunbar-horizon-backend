package com.example.DunbarHorizon.buzz.domain.repository;

import com.example.DunbarHorizon.buzz.domain.BuzzComment;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BuzzCommentRepository {
    BuzzComment save(BuzzComment buzzComment);
    Optional<BuzzComment> findById(Long commentId);
    List<BuzzComment> findAllByThreadId(Long threadId);
    void deleteById(Long commentId);
    void deleteAllByThreadId(Long threadId);
    void deleteAllByThreadIds(Collection<Long> threadIds);
}
