package com.example.DunbarHorizon.social.adapter.in.web;

import com.example.DunbarHorizon.social.application.dto.result.SocialProfileResult;
import com.example.DunbarHorizon.social.application.service.FriendshipQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/social/profiles")
@RequiredArgsConstructor
public class SocialUserController {

    private final FriendshipQueryService friendshipQueryService;

    @GetMapping("/{userId}")
    public ResponseEntity<SocialProfileResult> getSocialProfile(@PathVariable Long userId) {
        return ResponseEntity.ok(friendshipQueryService.getSocialProfile(userId));
    }
}
