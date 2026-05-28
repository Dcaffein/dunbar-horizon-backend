package com.example.DunbarHorizon.account.adapter.in.web;

import com.example.DunbarHorizon.account.application.service.DevUserService;
import com.example.DunbarHorizon.account.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("local")
@RestController
@RequestMapping("/api/dev")
@RequiredArgsConstructor
public class DevController {

    private final DevUserService devUserService;

    @PostMapping("/users")
    public ResponseEntity<DummyUserResponse> createDummyUser(@RequestBody DummyUserRequest request) {
        User user = devUserService.createDummyUser(request.email(), request.nickname());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new DummyUserResponse(user.getId(), user.getEmail(), user.getNickname()));
    }

    record DummyUserRequest(String email, String nickname) {}
    record DummyUserResponse(Long id, String email, String nickname) {}
}
