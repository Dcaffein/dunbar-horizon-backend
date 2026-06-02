package com.example.DunbarHorizon.social.adapter.in.web;

import com.example.DunbarHorizon.social.application.dto.result.SocialProfileResult;
import com.example.DunbarHorizon.social.application.port.in.SocialUserQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/social")
@RequiredArgsConstructor
public class SocialUserController {

    private final SocialUserQueryUseCase socialUserQueryUseCase;

    @GetMapping("/users/{id}")
    public ResponseEntity<SocialProfileResult> getSocialProfile(@PathVariable Long id) {
        return ResponseEntity.ok(socialUserQueryUseCase.getSocialProfile(id));
    }
}
