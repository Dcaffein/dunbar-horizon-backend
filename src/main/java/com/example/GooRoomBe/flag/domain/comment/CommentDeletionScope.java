package com.example.GooRoomBe.flag.domain.comment;

import lombok.Getter;

@Getter
public class CommentDeletionScope {

    private final Long targetId;
    private final boolean includeReplies;

    CommentDeletionScope(Long targetId, boolean includeReplies) {
        this.targetId = targetId;
        this.includeReplies = includeReplies;
    }
}