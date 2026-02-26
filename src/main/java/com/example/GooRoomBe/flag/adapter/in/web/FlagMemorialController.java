package com.example.GooRoomBe.flag.adapter.in.web;

import com.example.GooRoomBe.flag.adapter.in.web.dto.MemorialCreateRequest;
import com.example.GooRoomBe.flag.application.port.in.FlagMemorialCommandUseCase;
import com.example.GooRoomBe.flag.application.port.in.FlagMemorialQueryUseCase;
import com.example.GooRoomBe.flag.application.port.in.dto.MemorialResponse;
import com.example.GooRoomBe.flag.adapter.in.web.dto.MemorialUpdateRequest;
import com.example.GooRoomBe.global.annotation.CurrentUserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/flags")
@RequiredArgsConstructor
public class FlagMemorialController {

    private final FlagMemorialCommandUseCase memorialCommandUseCase;
    private final FlagMemorialQueryUseCase memorialQueryUseCase;

    @PostMapping("/{flagId}/memorials")
    public ResponseEntity<Long> createMemorial(
            @PathVariable Long flagId,
            @CurrentUserId Long currentUserId,
            @RequestBody @Valid MemorialCreateRequest request
    ) {
        Long memorialId = memorialCommandUseCase.createMemorial(
                flagId, currentUserId, request.content()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(memorialId);
    }

    @GetMapping("/{flagId}/memorials")
    public ResponseEntity<List<MemorialResponse>> getMemorials(
            @PathVariable Long flagId,
            @CurrentUserId Long currentUserId
    ) {
        List<MemorialResponse> responses = memorialQueryUseCase.getMemorials(flagId, currentUserId);
        return ResponseEntity.ok(responses);
    }

    @PatchMapping("/memorials/{id}")
    public ResponseEntity<Void> updateMemorial(
            @PathVariable Long id,
            @CurrentUserId Long currentUserId,
            @RequestBody @Valid MemorialUpdateRequest request
    ) {
        memorialCommandUseCase.updateMemorial(id, currentUserId, request.content());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/memorials/{id}")
    public ResponseEntity<Void> deleteMemorial(
            @PathVariable Long id,
            @CurrentUserId Long currentUserId
    ) {
        memorialCommandUseCase.deleteMemorial(id, currentUserId);
        return ResponseEntity.noContent().build();
    }
}