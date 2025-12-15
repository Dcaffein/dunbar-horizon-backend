package com.example.GooRoomBe.social.friend.api;

import com.example.GooRoomBe.social.friend.api.dto.FriendUpdateRequestDto;
import com.example.GooRoomBe.social.friend.application.FriendshipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/friends")
@RequiredArgsConstructor
public class FriendshipController {

    private final FriendshipService friendShipService;

    @PatchMapping("/{friendId}")
    public ResponseEntity<Void> updateFriend(
            @AuthenticationPrincipal String currentUserId,
            @PathVariable String friendId,
            @RequestBody @Valid FriendUpdateRequestDto friendUpdateRequestDto) {

        friendShipService.updateFriendProps(currentUserId, friendId, friendUpdateRequestDto);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{friendId}")
    public ResponseEntity<Void> deleteFriendship(
            @AuthenticationPrincipal String currentUserId,
            @PathVariable String friendId) {

        friendShipService.deleteFriendShip(currentUserId, friendId);
        return ResponseEntity.noContent().build();
    }
}