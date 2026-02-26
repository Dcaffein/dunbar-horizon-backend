package com.example.GooRoomBe.flag.application.port.in;

import com.example.GooRoomBe.flag.application.port.in.dto.CommentResponse;

import java.util.List;

public interface FlagCommentQueryUseCase {
    List<CommentResponse> getCommentTree(Long flagId, Long viewerId);
    Long getCommentCount(Long flagId);
}