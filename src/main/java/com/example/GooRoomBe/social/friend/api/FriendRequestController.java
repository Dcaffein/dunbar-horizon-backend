package com.example.GooRoomBe.social.friend.api;

import com.example.GooRoomBe.social.friend.api.dto.FriendRequestCreateDto;
import com.example.GooRoomBe.social.friend.api.dto.FriendRequestResponseDto;
import com.example.GooRoomBe.social.friend.api.dto.FriendRequestUpdateDto;
import com.example.GooRoomBe.social.friend.application.FriendRequestService;
import com.example.GooRoomBe.social.friend.domain.FriendRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/friend-requests")
@RequiredArgsConstructor
public class FriendRequestController {

    private final FriendRequestService friendRequestService;

    @PostMapping
    public ResponseEntity<FriendRequestResponseDto> sendFriendRequest(
            @AuthenticationPrincipal String currentUserId,
            @RequestBody @Valid FriendRequestCreateDto friendRequestCreateDto) {

        FriendRequest newRequest = friendRequestService.createFriendRequest(currentUserId, friendRequestCreateDto.receiverId());
        FriendRequestResponseDto response = FriendRequestResponseDto.from(newRequest);

        return ResponseEntity
                .created(URI.create("/api/v1/friend-requests/" + newRequest.getId()))
                .body(response);
    }

    @PatchMapping("/{requestId}")
    public ResponseEntity<Void> updateFriendRequest(
            @AuthenticationPrincipal String currentUserId,
            @PathVariable String requestId,
            @RequestBody @Valid FriendRequestUpdateDto friendRequestUpdateDto) {

        friendRequestService.updateFriendRequest(requestId, currentUserId, friendRequestUpdateDto.status());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{requestId}")
    public ResponseEntity<Void> deleteFriendRequest(
            @AuthenticationPrincipal String currentUserId,
            @PathVariable String requestId) {

        friendRequestService.cancelFriendRequest(requestId, currentUserId);
        return ResponseEntity.noContent().build();
    }
}