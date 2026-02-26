package com.example.GooRoomBe.cast.adapter.in.web;

import com.example.GooRoomBe.cast.adapter.in.web.dto.CastCreateRequestDto;
import com.example.GooRoomBe.cast.adapter.in.web.dto.CastDetailResponseDto;
import com.example.GooRoomBe.cast.adapter.in.web.dto.CastReplyRequestDto;
import com.example.GooRoomBe.cast.adapter.in.web.dto.CastSummaryResponseDto;
import com.example.GooRoomBe.cast.application.service.CastService;
import com.example.GooRoomBe.cast.domain.model.Cast;
import com.example.GooRoomBe.global.annotation.CurrentUserId;
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
@RequestMapping("/api/v1/casts")
@RequiredArgsConstructor
public class CastController {

    private final CastService castService;

    @PostMapping
    public ResponseEntity<Void> createCast(
            @CurrentUserId Long currentUserId,
            @RequestBody @Valid CastCreateRequestDto castCreateRequestDto) {
        castService.createCast(castCreateRequestDto.toCommand(currentUserId));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/")
    public ResponseEntity<Slice<CastSummaryResponseDto>> getReceivedCasts(
            @CurrentUserId Long currentUserId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Slice<Cast> casts = castService.getReceivedCasts(currentUserId, pageable);
        Slice<CastSummaryResponseDto> response = casts.map(cast -> CastSummaryResponseDto.from(cast, currentUserId));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{castId}")
    public ResponseEntity<CastDetailResponseDto> getCastDetail(
            @CurrentUserId Long currentUserId,
            @PathVariable String castId) {
        Cast cast = castService.getCastDetail(currentUserId, castId);
        return ResponseEntity.ok(CastDetailResponseDto.from(cast, currentUserId));
    }

    @GetMapping("/senders/unread")
    public ResponseEntity<List<Long>> getUnreadSenders(@CurrentUserId Long currentUserId) {
        return ResponseEntity.ok(castService.getUnreadSenderIds(currentUserId));
    }

    @PostMapping("/{castId}/replies")
    public ResponseEntity<Void> reply(
            @CurrentUserId Long currentUserId,
            @PathVariable String castId,
            @RequestBody @Valid CastReplyRequestDto dto) {
        castService.replyToCast(currentUserId, castId, dto.text(),dto.imageUrls(), dto.isPublic());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PatchMapping("/{castId}/replies/{replyId}")
    public ResponseEntity<Void> updateReply(
            @CurrentUserId Long currentUserId,
            @PathVariable String castId,
            @PathVariable String replyId,
            @RequestBody @Valid CastReplyRequestDto dto) {
        castService.updateReply(currentUserId, castId, replyId,  dto.text(),dto.imageUrls());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{castId}/replies/{replyId}")
    public ResponseEntity<Void> deleteReply(
            @CurrentUserId Long currentUserId,
            @PathVariable String castId,
            @PathVariable String replyId) {
        castService.deleteReply(currentUserId, castId, replyId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{castId}")
    public ResponseEntity<Void> deleteCast(
            @CurrentUserId Long currentUserId,
            @PathVariable String castId) {
        castService.deleteCast(currentUserId, castId);
        return ResponseEntity.noContent().build();
    }
}