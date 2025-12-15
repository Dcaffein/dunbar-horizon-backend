package com.example.GooRoomBe.account.user.api;

import com.example.GooRoomBe.account.auth.application.LocalAccountService;
import com.example.GooRoomBe.account.user.api.dto.UserSignupRequestDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final LocalAccountService localAccountService;

    @PostMapping
    public ResponseEntity<Void> signup(@RequestBody @Valid UserSignupRequestDto dto) {
        localAccountService.signUp(dto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
