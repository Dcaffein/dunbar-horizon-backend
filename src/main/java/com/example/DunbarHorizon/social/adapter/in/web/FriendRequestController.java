package com.example.DunbarHorizon.social.adapter.in.web;

import com.example.DunbarHorizon.global.annotation.CurrentUserId;
import com.example.DunbarHorizon.social.adapter.in.web.dto.FriendRequestCreateRequest;
import com.example.DunbarHorizon.social.adapter.in.web.dto.FriendRequestStatusUpdateRequest;
import com.example.DunbarHorizon.social.application.dto.result.FriendRequestResult;
import com.example.DunbarHorizon.social.application.dto.FriendRequestDirection;
import com.example.DunbarHorizon.social.application.service.FriendRequestService;
import com.example.DunbarHorizon.social.domain.friend.FriendRequestStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/friend-requests")
@RequiredArgsConstructor
public class FriendRequestController {

    private final FriendRequestService friendRequestService;

    @GetMapping
    public ResponseEntity<List<FriendRequestResult>> getRequests(
            @CurrentUserId Long currentUserId,
            @RequestParam String direction,
            @RequestParam(required = false) FriendRequestStatus status) {

        return ResponseEntity.ok(friendRequestService.getRequests(
                currentUserId, FriendRequestDirection.from(direction), status));
    }

    @PostMapping
    public ResponseEntity<FriendRequestResult> sendFriendRequest(
            @CurrentUserId Long currentUserId,
            @RequestBody @Valid FriendRequestCreateRequest request) {

        FriendRequestResult response = friendRequestService.sendRequest(currentUserId, request.receiverId());

        return ResponseEntity
                .created(URI.create(
                        "/api/v1/friend-requests/" + response.receiver().id()))
                .body(response);
    }

    @DeleteMapping("/{counterpartId}")
    public ResponseEntity<Void> cancelFriendRequest(
            @CurrentUserId Long currentUserId,
            @PathVariable Long counterpartId) {

        friendRequestService.cancelRequest(currentUserId, counterpartId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{counterpartId}")
    public ResponseEntity<Void> updateFriendRequestStatus(
            @CurrentUserId Long currentUserId,
            @PathVariable Long counterpartId,
            @RequestBody @Valid FriendRequestStatusUpdateRequest request) {

        friendRequestService.updateStatus(currentUserId, counterpartId, request.status());
        return ResponseEntity.noContent().build();
    }
}
