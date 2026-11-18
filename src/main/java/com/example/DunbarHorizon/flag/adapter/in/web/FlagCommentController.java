package com.example.DunbarHorizon.flag.adapter.in.web;

import com.example.DunbarHorizon.flag.adapter.in.web.dto.CommentCreateRequest;
import com.example.DunbarHorizon.flag.adapter.in.web.dto.CommentUpdateRequest;
import com.example.DunbarHorizon.flag.application.service.comment.FlagCommentCommandService;
import com.example.DunbarHorizon.flag.application.service.comment.FlagCommentQueryService;
import com.example.DunbarHorizon.flag.application.dto.result.CommentResult;
import com.example.DunbarHorizon.global.annotation.CurrentUserId;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/flags/{flagId}/comments")
@RequiredArgsConstructor
public class FlagCommentController {

    private final FlagCommentCommandService flagCommentCommandService;
    private final FlagCommentQueryService flagCommentQueryService;

    @GetMapping
    public ResponseEntity<List<CommentResult>> getComments(
            @PathVariable Long flagId,
            @CurrentUserId Long currentUserId
    ) {
        List<CommentResult> commentTree = flagCommentQueryService.getCommentTree(flagId, currentUserId);
        return ResponseEntity.ok(commentTree);
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getCommentCount(@PathVariable Long flagId) {
        return ResponseEntity.ok(flagCommentQueryService.getCommentCount(flagId));
    }

    @PostMapping
    public ResponseEntity<Long> createRootComment(
            @PathVariable Long flagId,
            @CurrentUserId Long currentUserId,
            @Valid @RequestBody CommentCreateRequest request
    ) {
        Long commentId = flagCommentCommandService.createRootComment(
                flagId, currentUserId, request.content(), request.isPrivate()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(commentId);
    }

    @PostMapping("/{parentId}/replies")
    public ResponseEntity<Long> createReply(
            @PathVariable Long flagId,
            @PathVariable Long parentId,
            @CurrentUserId Long currentUserId,
            @Valid @RequestBody CommentCreateRequest request
    ) {
        Long replyId = flagCommentCommandService.createReply(
                flagId, parentId, currentUserId, request.content(), request.isPrivate()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(replyId);
    }

    @PatchMapping("/{commentId}")
    public ResponseEntity<Void> updateComment(
            @PathVariable Long flagId,
            @PathVariable Long commentId,
            @CurrentUserId Long currentUserId,
            @Valid @RequestBody CommentUpdateRequest request
    ) {
        flagCommentCommandService.updateComment(
                flagId, commentId, currentUserId, request.content(), request.isPrivate()
        );
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long flagId,
            @PathVariable Long commentId,
            @CurrentUserId Long currentUserId
    ) {
        flagCommentCommandService.deleteComment(flagId, commentId, currentUserId);
        return ResponseEntity.noContent().build();
    }
}
