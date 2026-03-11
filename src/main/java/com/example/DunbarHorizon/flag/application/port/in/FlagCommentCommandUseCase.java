package com.example.DunbarHorizon.flag.application.port.in;

public interface FlagCommentCommandUseCase {
    Long createRootComment(Long flagId, Long userId, String content, boolean isPrivate);
    Long createReply(Long parentId, Long userId, String content, boolean isPrivate);
    void updateComment(Long commentId, Long userId, String content, boolean isPrivate);
    void deleteComment(Long commentId, Long userId);
}
