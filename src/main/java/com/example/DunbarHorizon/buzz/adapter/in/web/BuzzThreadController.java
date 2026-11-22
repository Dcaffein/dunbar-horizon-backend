package com.example.DunbarHorizon.buzz.adapter.in.web;

import com.example.DunbarHorizon.buzz.adapter.in.web.dto.BuzzPostRequest;
import com.example.DunbarHorizon.buzz.adapter.in.web.dto.BuzzThreadCreateRequest;
import com.example.DunbarHorizon.buzz.application.dto.command.UpdateBuzzCommentCommand;
import com.example.DunbarHorizon.buzz.application.dto.command.UpdateBuzzThreadCommand;
import com.example.DunbarHorizon.buzz.application.dto.command.WriteBuzzCommentCommand;
import com.example.DunbarHorizon.buzz.application.dto.result.BuzzThreadDetailResult;
import com.example.DunbarHorizon.buzz.application.service.BuzzCommentService;
import com.example.DunbarHorizon.buzz.application.service.BuzzThreadQueryService;
import com.example.DunbarHorizon.buzz.application.service.BuzzThreadService;
import com.example.DunbarHorizon.global.annotation.CurrentUserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/buzzes/{buzzId}/threads")
@RequiredArgsConstructor
public class BuzzThreadController {

    private final BuzzThreadService buzzThreadService;
    private final BuzzThreadQueryService buzzThreadQueryService;
    private final BuzzCommentService buzzCommentService;

    @PostMapping
    public ResponseEntity<Void> createThread(
            @CurrentUserId Long currentUserId,
            @PathVariable Long buzzId,
            @RequestBody @Valid BuzzThreadCreateRequest request) {
        buzzThreadService.createThread(request.toCommand(currentUserId, buzzId));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/{threadId}")
    public ResponseEntity<BuzzThreadDetailResult> getThread(
            @CurrentUserId Long currentUserId,
            @PathVariable Long buzzId,
            @PathVariable Long threadId) {
        return ResponseEntity.ok(buzzThreadQueryService.getThread(currentUserId, buzzId, threadId));
    }

    @PatchMapping("/{threadId}")
    public ResponseEntity<Void> updateThread(
            @CurrentUserId Long currentUserId,
            @PathVariable Long buzzId,
            @PathVariable Long threadId,
            @RequestBody @Valid BuzzPostRequest request) {
        buzzThreadService.updateThread(new UpdateBuzzThreadCommand(
                currentUserId, buzzId, threadId, request.text(), request.imageKeysOrEmpty()));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{threadId}")
    public ResponseEntity<Void> deleteThread(
            @CurrentUserId Long currentUserId,
            @PathVariable Long buzzId,
            @PathVariable Long threadId) {
        buzzThreadService.deleteThread(currentUserId, buzzId, threadId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{threadId}/comments")
    public ResponseEntity<Void> writeComment(
            @CurrentUserId Long currentUserId,
            @PathVariable Long buzzId,
            @PathVariable Long threadId,
            @RequestBody @Valid BuzzPostRequest request) {
        buzzCommentService.writeComment(new WriteBuzzCommentCommand(
                currentUserId, buzzId, threadId, request.text(), request.imageKeysOrEmpty()));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PatchMapping("/{threadId}/comments/{commentId}")
    public ResponseEntity<Void> updateComment(
            @CurrentUserId Long currentUserId,
            @PathVariable Long buzzId,
            @PathVariable Long threadId,
            @PathVariable Long commentId,
            @RequestBody @Valid BuzzPostRequest request) {
        buzzCommentService.updateComment(new UpdateBuzzCommentCommand(
                currentUserId, buzzId, threadId, commentId, request.text(), request.imageKeysOrEmpty()));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{threadId}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @CurrentUserId Long currentUserId,
            @PathVariable Long buzzId,
            @PathVariable Long threadId,
            @PathVariable Long commentId) {
        buzzCommentService.deleteComment(currentUserId, buzzId, threadId, commentId);
        return ResponseEntity.noContent().build();
    }
}
