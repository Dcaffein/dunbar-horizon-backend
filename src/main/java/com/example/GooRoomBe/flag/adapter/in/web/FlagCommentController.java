package com.example.GooRoomBe.flag.adapter.in.web;

import com.example.GooRoomBe.flag.adapter.in.web.dto.CommentCreateRequest;
import com.example.GooRoomBe.flag.adapter.in.web.dto.CommentUpdateRequest;
import com.example.GooRoomBe.flag.application.port.in.FlagCommentCommandUseCase;
import com.example.GooRoomBe.flag.application.port.in.FlagCommentQueryUseCase;
import com.example.GooRoomBe.flag.application.port.in.dto.CommentResponse;
import com.example.GooRoomBe.global.annotation.CurrentUserId;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class FlagCommentController {

    private final FlagCommentCommandUseCase flagCommentCommandUseCase;
    private final FlagCommentQueryUseCase commentQueryUseCase;

    @GetMapping("/flags/{flagId}/comments")
    public ResponseEntity<List<CommentResponse>> getComments(
            @PathVariable Long flagId,
            @CurrentUserId Long currentUserId
    ) {
        List<CommentResponse> commentTree = commentQueryUseCase.getCommentTree(flagId, currentUserId);
        return ResponseEntity.ok(commentTree);
    }

    @GetMapping("/flags/{flagId}/comments/count")
    public ResponseEntity<Long> getCommentCount(@PathVariable Long flagId) {
        return ResponseEntity.ok(commentQueryUseCase.getCommentCount(flagId));
    }

    @PostMapping("/flags/{flagId}/comments")
    public ResponseEntity<Long> createRootComment(
            @PathVariable Long flagId,
            @CurrentUserId Long currentUserId,
            @RequestBody CommentCreateRequest request
    ) {
        Long commentId = flagCommentCommandUseCase.createRootComment(
                flagId, currentUserId, request.content(), request.isPrivate()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(commentId);
    }

    @PostMapping("/comments/{parentId}/replies")
    public ResponseEntity<Long> createReply(
            @PathVariable Long parentId,
            @CurrentUserId Long currentUserId,
            @RequestBody CommentCreateRequest request
    ) {
        Long replyId = flagCommentCommandUseCase.createReply(
                parentId, currentUserId, request.content(), request.isPrivate()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(replyId);
    }

    @PatchMapping("/comments/{commentId}")
    public ResponseEntity<Void> updateComment(
            @PathVariable Long commentId,
            @CurrentUserId Long currentUserId,
            @RequestBody CommentUpdateRequest request
    ) {
        flagCommentCommandUseCase.updateComment(
                commentId, currentUserId, request.content(), request.isPrivate()
        );
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long commentId,
            @CurrentUserId Long currentUserId
    ) {
        flagCommentCommandUseCase.deleteComment(commentId, currentUserId);
        return ResponseEntity.noContent().build();
    }
}