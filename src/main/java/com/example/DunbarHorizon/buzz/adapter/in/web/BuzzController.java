package com.example.DunbarHorizon.buzz.adapter.in.web;

import com.example.DunbarHorizon.buzz.adapter.in.web.dto.BuzzCreateRequest;
import com.example.DunbarHorizon.buzz.adapter.in.web.dto.BuzzUpdateRequest;
import com.example.DunbarHorizon.buzz.application.service.BuzzQueryService;
import com.example.DunbarHorizon.buzz.application.service.BuzzService;
import com.example.DunbarHorizon.buzz.application.dto.result.BuzzDetailResult;
import com.example.DunbarHorizon.buzz.application.dto.result.BuzzSummaryResult;
import com.example.DunbarHorizon.buzz.application.port.out.ImageStoragePort;
import com.example.DunbarHorizon.global.annotation.CurrentUserId;
import com.example.DunbarHorizon.global.imageStorage.PresignRequest;
import com.example.DunbarHorizon.global.imageStorage.PresignedUploadResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/buzzes")
@RequiredArgsConstructor
public class BuzzController {

    private final BuzzService buzzService;
    private final BuzzQueryService buzzQueryService;
    private final ImageStoragePort imageStoragePort;

    @PostMapping("/images/presign")
    public ResponseEntity<List<PresignedUploadResult>> presignImages(
            @CurrentUserId Long currentUserId,
            @RequestBody List<PresignRequest> requests) {
        return ResponseEntity.ok(imageStoragePort.presignUploads(requests));
    }

    @PostMapping
    public ResponseEntity<Void> createBuzz(
            @CurrentUserId Long currentUserId,
            @RequestBody @Valid BuzzCreateRequest request) {
        List<String> imageKeys = request.imageKeys() != null ? request.imageKeys() : List.of();
        buzzService.createBuzz(request.toCommand(currentUserId), imageKeys);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/")
    public ResponseEntity<Slice<BuzzSummaryResult>> getReceivedBuzzes(
            @CurrentUserId Long currentUserId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(buzzQueryService.getReceivedBuzzes(currentUserId, pageable));
    }

    @GetMapping("/sent")
    public ResponseEntity<Slice<BuzzSummaryResult>> getSentBuzzes(
            @CurrentUserId Long currentUserId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(buzzQueryService.getSentBuzzes(currentUserId, pageable));
    }

    @GetMapping("/{buzzId}")
    public ResponseEntity<BuzzDetailResult> getBuzzDetail(
            @CurrentUserId Long currentUserId,
            @PathVariable Long buzzId) {
        return ResponseEntity.ok(buzzQueryService.getBuzzDetail(currentUserId, buzzId));
    }

    @PutMapping("/{buzzId}/readers/me")
    public ResponseEntity<Void> readBuzz(
            @CurrentUserId Long currentUserId,
            @PathVariable Long buzzId) {
        buzzService.readBuzz(currentUserId, buzzId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/senders/unread")
    public ResponseEntity<List<Long>> getUnreadSenders(@CurrentUserId Long currentUserId) {
        return ResponseEntity.ok(buzzQueryService.getUnreadSenderIds(currentUserId));
    }

    @PatchMapping("/{buzzId}")
    public ResponseEntity<Void> updateBuzz(
            @CurrentUserId Long currentUserId,
            @PathVariable Long buzzId,
            @RequestBody @Valid BuzzUpdateRequest request) {
        buzzService.updateBuzz(request.toCommand(currentUserId, buzzId));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{buzzId}")
    public ResponseEntity<Void> deleteBuzz(
            @CurrentUserId Long currentUserId,
            @PathVariable Long buzzId) {
        buzzService.deleteBuzz(currentUserId, buzzId);
        return ResponseEntity.noContent().build();
    }
}
