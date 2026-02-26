package com.example.GooRoomBe.account.adapter.in.web;

import com.example.GooRoomBe.account.adapter.in.web.cookie.AuthCookieManager;
import com.example.GooRoomBe.account.application.port.in.LoginUseCase;
import com.example.GooRoomBe.account.application.port.in.SignupUseCase;
import com.example.GooRoomBe.account.application.port.in.VerificationUseCase;
import com.example.GooRoomBe.account.application.port.in.dto.AuthTokenResult;
import com.example.GooRoomBe.account.adapter.in.web.dto.LoginRequestDto;
import com.example.GooRoomBe.account.adapter.in.web.dto.SignupRequestDto;
import com.example.GooRoomBe.account.adapter.in.web.dto.VerificationEmailRequestDto;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AccountController {

    private final SignupUseCase signupUseCase;
    private final LoginUseCase loginUseCase;
    private final VerificationUseCase verificationUseCase;
    private final AuthCookieManager authCookieManager;

    @PostMapping("/users")
    public ResponseEntity<Void> signup(@RequestBody @Valid SignupRequestDto dto) {
        signupUseCase.signup(dto.email(), dto.password(), dto.nickname());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/tokens")
    public ResponseEntity<Void> login(@RequestBody @Valid LoginRequestDto loginRequestDto,
                                      HttpServletResponse response) {
        AuthTokenResult jwts = loginUseCase.login(loginRequestDto.email(), loginRequestDto.password());
        handleTokenResponse(response, jwts);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/tokens")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            HttpServletResponse response) {
        if (refreshToken != null) {
            loginUseCase.logout(refreshToken);
        }
        authCookieManager.addExpiredTokenCookie(response);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/tokens")
    public ResponseEntity<Void> reissue(
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            HttpServletResponse response) {
        AuthTokenResult newJwts = loginUseCase.reissue(refreshToken);
        handleTokenResponse(response, newJwts);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/verifications")
    public ResponseEntity<Void> sendVerificationEmail(
            @RequestBody @Valid VerificationEmailRequestDto verificationEmailRequestDto) {
        verificationUseCase.sendVerificationEmail(verificationEmailRequestDto.email(), verificationEmailRequestDto.redirectPage());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PatchMapping("/verifications")
    public ResponseEntity<Void> verifyEmail(@RequestParam String token) {
        verificationUseCase.verifyEmail(token);
        return ResponseEntity.ok().build();
    }

    private void handleTokenResponse(HttpServletResponse response, AuthTokenResult tokens) {
        authCookieManager.addAccessTokenCookie(response, tokens.accessToken());
        authCookieManager.addRefreshTokenCookie(response, tokens.refreshToken());
    }
}