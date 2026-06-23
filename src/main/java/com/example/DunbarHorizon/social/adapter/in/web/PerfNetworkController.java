package com.example.DunbarHorizon.social.adapter.in.web;

import com.example.DunbarHorizon.social.application.dto.result.NetworkGraphResult;
import com.example.DunbarHorizon.social.application.port.in.SocialNetworkQueryUseCase;
import com.example.DunbarHorizon.social.domain.friend.DunbarCircle;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Profile("perf")
@RequestMapping("/api/perf/v1/networks")
public class PerfNetworkController {

    private final SocialNetworkQueryUseCase networkQueryUseCase;

    @GetMapping("/{userId}")
    public ResponseEntity<NetworkGraphResult> getFriendsNetwork(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "DUNBAR") DunbarCircle circleSize
    ) {
        return ResponseEntity.ok(networkQueryUseCase.getFriendsNetwork(userId, circleSize));
    }
}
