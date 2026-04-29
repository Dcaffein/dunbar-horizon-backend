package com.example.DunbarHorizon.buzz.adapter.in.web;

import com.example.DunbarHorizon.buzz.adapter.in.web.dto.BuzzCreateRequest;
import com.example.DunbarHorizon.buzz.adapter.in.web.dto.BuzzReplyRequest;
import com.example.DunbarHorizon.buzz.application.port.in.BuzzCommandUseCase;
import com.example.DunbarHorizon.buzz.application.port.in.BuzzQueryUseCase;
import com.example.DunbarHorizon.buzz.application.dto.result.BuzzDetailResult;
import com.example.DunbarHorizon.buzz.application.dto.result.BuzzSummaryResult;
import com.example.DunbarHorizon.global.annotation.CurrentUserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/buzzes")
@RequiredArgsConstructor
public class BuzzController {

    private final BuzzCommandUseCase buzzCommandUseCase;
    private final BuzzQueryUseCase buzzQueryUseCase;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> createBuzz(
            @CurrentUserId Long currentUserId,
            @RequestPart("request") @Valid BuzzCreateRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        buzzCommandUseCase.createBuzz(request.toCommand(currentUserId), images);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/")
    public ResponseEntity<Slice<BuzzSummaryResult>> getReceivedBuzzes(
            @CurrentUserId Long currentUserId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(buzzQueryUseCase.getReceivedBuzzes(currentUserId, pageable));
    }

    @GetMapping("/{buzzId}")
    public ResponseEntity<BuzzDetailResult> getBuzzDetail(
            @CurrentUserId Long currentUserId,
            @PathVariable String buzzId) {
        return ResponseEntity.ok(buzzQueryUseCase.getBuzzDetail(currentUserId, buzzId));
    }

    @GetMapping("/senders/unread")
    public ResponseEntity<List<Long>> getUnreadSenders(@CurrentUserId Long currentUserId) {
        return ResponseEntity.ok(buzzQueryUseCase.getUnreadSenderIds(currentUserId));
    }

    @PostMapping(value = "/{buzzId}/replies", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> reply(
            @CurrentUserId Long currentUserId,
            @PathVariable String buzzId,
            @RequestPart("request") @Valid BuzzReplyRequest dto,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        buzzCommandUseCase.replyToBuzz(currentUserId, buzzId, dto.text(), images, dto.isPublic());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PatchMapping(value = "/{buzzId}/replies/{replyId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> updateReply(
            @CurrentUserId Long currentUserId,
            @PathVariable String buzzId,
            @PathVariable String replyId,
            @RequestPart("request") @Valid BuzzReplyRequest dto,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        buzzCommandUseCase.updateReply(currentUserId, buzzId, replyId, dto.text(), images);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{buzzId}/replies/{replyId}")
    public ResponseEntity<Void> deleteReply(
            @CurrentUserId Long currentUserId,
            @PathVariable String buzzId,
            @PathVariable String replyId) {
        buzzCommandUseCase.deleteReply(currentUserId, buzzId, replyId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{buzzId}")
    public ResponseEntity<Void> deleteBuzz(
            @CurrentUserId Long currentUserId,
            @PathVariable String buzzId) {
        buzzCommandUseCase.deleteBuzz(currentUserId, buzzId);
        return ResponseEntity.noContent().build();
    }
}
