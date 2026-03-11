package com.example.DunbarHorizon.flag.application.port.in;

import com.example.DunbarHorizon.flag.application.dto.result.CommentResult;

import java.util.List;

public interface FlagCommentQueryUseCase {
    List<CommentResult> getCommentTree(Long flagId, Long viewerId);
    Long getCommentCount(Long flagId);
}