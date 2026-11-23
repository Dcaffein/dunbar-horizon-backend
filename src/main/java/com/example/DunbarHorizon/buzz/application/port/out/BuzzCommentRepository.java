package com.example.DunbarHorizon.buzz.application.port.out;

import com.example.DunbarHorizon.buzz.domain.BuzzComment;

import java.util.Optional;

public interface BuzzCommentRepository {
    BuzzComment save(BuzzComment buzzComment);
    Optional<BuzzComment> findById(Long commentId);
    void deleteById(Long commentId);
    void deleteAllByThreadId(Long threadId);
}
