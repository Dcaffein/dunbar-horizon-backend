package com.example.GooRoomBe.flag.domain.comment.repository;

import com.example.GooRoomBe.flag.domain.comment.CommentDeletionScope;
import com.example.GooRoomBe.flag.domain.comment.FlagComment;

import java.util.List;
import java.util.Optional;

public interface FlagCommentRepository {
    FlagComment save(FlagComment comment);
    Optional<FlagComment> findById(Long id);
    List<FlagComment> findAllByFlagId(Long flagId);
    Optional<FlagComment> findByIdForUpdate(Long id);
    void delete(CommentDeletionScope scope);
}