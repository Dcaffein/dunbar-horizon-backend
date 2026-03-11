package com.example.DunbarHorizon.flag.adapter.out.persistence;

import com.example.DunbarHorizon.flag.adapter.out.persistence.jpa.FlagCommentJpaRepository;
import com.example.DunbarHorizon.flag.domain.comment.CommentDeletionScope;
import com.example.DunbarHorizon.flag.domain.comment.FlagComment;
import com.example.DunbarHorizon.flag.domain.comment.repository.FlagCommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class FlagCommentRepositoryAdapter implements FlagCommentRepository {

    private final FlagCommentJpaRepository jpaRepository;

    @Override
    public FlagComment save(FlagComment comment) {
        return jpaRepository.save(comment);
    }

    @Override
    public Optional<FlagComment> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<FlagComment> findAllByFlagId(Long flagId) {
        return jpaRepository.findAllByFlagId(flagId);
    }

    @Override
    public Optional<FlagComment> findByIdForUpdate(Long id) {
        return jpaRepository.findByIdForUpdate(id);
    }

    @Override
    public void delete(CommentDeletionScope scope) {
        if (scope.isIncludeReplies()) {
            jpaRepository.deleteTargetAndReplies(scope.getTargetId());
        } else {
            jpaRepository.deleteById(scope.getTargetId());
        }
    }
}