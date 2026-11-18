package com.example.DunbarHorizon.social.adapter.in.web;

import com.example.DunbarHorizon.global.annotation.CurrentUserId;
import com.example.DunbarHorizon.social.adapter.in.web.dto.LabelCreateRequest;
import com.example.DunbarHorizon.social.adapter.in.web.dto.LabelMemberAddRequest;
import com.example.DunbarHorizon.social.adapter.in.web.dto.LabelMembersReplaceRequest;
import com.example.DunbarHorizon.social.adapter.in.web.dto.LabelUpdateRequest;
import com.example.DunbarHorizon.social.application.service.LabelCommandService;
import com.example.DunbarHorizon.social.application.service.LabelQueryService;
import com.example.DunbarHorizon.social.application.dto.result.LabelMemberResult;
import com.example.DunbarHorizon.social.application.dto.result.LabelResult;
import com.example.DunbarHorizon.social.domain.label.Label;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/labels")
@RequiredArgsConstructor
public class LabelController {

    private final LabelCommandService labelCommandService;
    private final LabelQueryService labelQueryService;

    @GetMapping
    public ResponseEntity<List<LabelResult>> getAllLabels(
            @CurrentUserId Long currentUserId,
            @RequestParam(required = false) Long memberId) {

        if (memberId != null) {
            return ResponseEntity.ok(labelQueryService.getLabelsByMember(currentUserId, memberId));
        }
        return ResponseEntity.ok(labelQueryService.getAllLabels(currentUserId));
    }

    @GetMapping("/{labelId}")
    public ResponseEntity<LabelResult> getLabelById(
            @CurrentUserId Long currentUserId,
            @PathVariable String labelId) {

        return ResponseEntity.ok(labelQueryService.getLabelById(currentUserId, labelId));
    }

    @GetMapping("/{labelId}/members")
    public ResponseEntity<List<LabelMemberResult>> getLabelMembers(
            @CurrentUserId Long currentUserId,
            @PathVariable String labelId) {

        return ResponseEntity.ok(labelQueryService.getLabelMembers(currentUserId, labelId));
    }

    @PostMapping
    public ResponseEntity<LabelResult> createLabel(
            @CurrentUserId Long currentUserId,
            @RequestBody @Valid LabelCreateRequest dto) {

        Label newLabel = labelCommandService.createLabel(currentUserId, dto.labelName());

        return ResponseEntity
                .created(URI.create("/api/v1/labels/" + newLabel.getId()))
                .body(LabelResult.created(newLabel));
    }

    @DeleteMapping("/{labelId}")
    public ResponseEntity<Void> deleteLabel(
            @CurrentUserId Long currentUserId,
            @PathVariable String labelId) {

        labelCommandService.deleteLabel(currentUserId, labelId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{labelId}")
    public ResponseEntity<Void> updateLabel(
            @CurrentUserId Long currentUserId,
            @PathVariable String labelId,
            @RequestBody @Valid LabelUpdateRequest dto) {

        labelCommandService.updateLabel(labelId, currentUserId, dto.labelName());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{labelId}/members")
    public ResponseEntity<Void> replaceMembers(
            @CurrentUserId Long currentUserId,
            @PathVariable String labelId,
            @RequestBody @Valid LabelMembersReplaceRequest labelMembersReplaceRequest) {

        labelCommandService.replaceLabelMembers(currentUserId, labelId, labelMembersReplaceRequest.memberIds());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{labelId}/members")
    public ResponseEntity<Void> addMember(
            @CurrentUserId Long currentUserId,
            @PathVariable String labelId,
            @RequestBody @Valid LabelMemberAddRequest labelMemberAddRequest) {

        labelCommandService.addMemberToLabel(currentUserId, labelId, labelMemberAddRequest.memberId());
        return ResponseEntity
                .created(URI.create("/api/v1/labels/" + labelId + "/members/" + labelMemberAddRequest.memberId()))
                .build();
    }

    @DeleteMapping("/{labelId}/members/{memberId}")
    public ResponseEntity<Void> removeMember(
            @CurrentUserId Long currentUserId,
            @PathVariable String labelId,
            @PathVariable Long memberId) {

        labelCommandService.removeMemberFromLabel(currentUserId, labelId, memberId);
        return ResponseEntity.noContent().build();
    }
}
