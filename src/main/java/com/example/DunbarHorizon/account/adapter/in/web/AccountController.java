package com.example.DunbarHorizon.account.adapter.in.web;

import com.example.DunbarHorizon.account.application.service.LoginService;
import com.example.DunbarHorizon.account.application.service.SignupService;
import com.example.DunbarHorizon.account.application.service.VerificationService;
import com.example.DunbarHorizon.account.application.dto.AuthTokenResult;
import com.example.DunbarHorizon.account.adapter.in.web.dto.LoginRequestDto;
import com.example.DunbarHorizon.account.adapter.in.web.dto.LogoutRequest;
import com.example.DunbarHorizon.account.adapter.in.web.dto.SignupRequestDto;
import com.example.DunbarHorizon.account.adapter.in.web.dto.VerificationEmailRequestDto;
import com.example.DunbarHorizon.account.adapter.in.web.dto.VerificationTokenResponse;
import com.example.DunbarHorizon.global.security.AuthCookieManager;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AccountController {

    private final SignupService signupService;
    private final LoginService loginService;
    private final VerificationService verificationService;
    private final AuthCookieManager authCookieManager;

    @PostMapping("/users")
    public ResponseEntity<Void> signup(@RequestBody @Valid SignupRequestDto dto,
                                       HttpServletResponse response) {
        AuthTokenResult jwts = signupService.signup(dto.token(), dto.password(), dto.nickname());
        addTokenResponse(response, jwts);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/tokens")
    public ResponseEntity<Void> login(@RequestBody @Valid LoginRequestDto loginRequestDto,
                                      HttpServletResponse response) {
        AuthTokenResult jwts = loginService.login(loginRequestDto.email(), loginRequestDto.password());
        addTokenResponse(response, jwts);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/tokens")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            @RequestBody(required = false) LogoutRequest logoutRequest,
            HttpServletResponse response) {
        String fcmToken = logoutRequest != null ? logoutRequest.fcmToken() : null;
        loginService.logout(refreshToken, fcmToken);
        authCookieManager.addExpiredTokenCookie(response);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/tokens")
    public ResponseEntity<Void> reissue(
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            HttpServletResponse response) {
        AuthTokenResult newJwts = loginService.reissue(refreshToken);
        addTokenResponse(response, newJwts);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/verifications")
    public ResponseEntity<Void> requestVerification(
            @RequestBody @Valid VerificationEmailRequestDto verificationEmailRequestDto) {
        verificationService.requestVerification(verificationEmailRequestDto.email());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * 토큰 유효성 확인. 프론트가 비밀번호 입력 폼을 그리기 전에 호출한다.
     * 없으면 사용자가 폼을 다 채워 제출한 뒤에야 만료를 알게 된다.
     */
    @GetMapping("/verifications/{token}")
    public ResponseEntity<VerificationTokenResponse> resolveVerification(@PathVariable String token) {
        return ResponseEntity.ok(new VerificationTokenResponse(verificationService.resolveEmail(token)));
    }

    private void addTokenResponse(HttpServletResponse response, AuthTokenResult tokens) {
        authCookieManager.addAccessTokenCookie(response, tokens.accessToken());
        authCookieManager.addRefreshTokenCookie(response, tokens.refreshToken());
    }
}
