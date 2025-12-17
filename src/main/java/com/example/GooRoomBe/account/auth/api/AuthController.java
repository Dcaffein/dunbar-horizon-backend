package com.example.GooRoomBe.account.auth.api;

import com.example.GooRoomBe.account.auth.api.dto.VerificationEmailSendRequestDto;
import com.example.GooRoomBe.account.auth.application.AuthTokenService;
import com.example.GooRoomBe.account.auth.application.LocalAccountService;
import com.example.GooRoomBe.account.auth.security.core.cookie.AuthCookieManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final LocalAccountService localAccountService;
    private final AuthTokenService authTokenService;

    private final AuthCookieManager authCookieManager;

    @GetMapping("/email-verifications")
    public ResponseEntity<Void> verifyEmail(@RequestParam("token") String token) {
        localAccountService.verifyEmail(token);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/email-verifications")
    public ResponseEntity<Void> sendVerificationEmail(@RequestBody @Valid VerificationEmailSendRequestDto dto) {
        localAccountService.sendVerificationEmail(dto.email(), dto.redirectPage());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/tokens")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "refreshToken", required = false) String refreshTokenValue,
            HttpServletResponse response) {

        if (refreshTokenValue != null) {
            authTokenService.logout(refreshTokenValue);
        }

        authCookieManager.clearAuthCookies(response);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/tokens")
    public ResponseEntity<Void> reissueTokens(
            HttpServletRequest request,
            HttpServletResponse response,
            @CookieValue(name = "refreshToken", required = false) String refreshTokenValue) {

        if (refreshTokenValue == null) {
            return ResponseEntity.badRequest().build();
        }

        Map<String, String> tokens = authTokenService.reissueTokens(refreshTokenValue);

        authCookieManager.addAuthCookies(
                response,
                tokens.get("accessToken"),
                tokens.get("refreshToken")
        );

        return ResponseEntity.ok().build();
    }
}