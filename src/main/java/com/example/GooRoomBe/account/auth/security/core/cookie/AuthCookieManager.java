package com.example.GooRoomBe.account.auth.security.core.cookie;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class AuthCookieManager {

    @Value("${jwt.access-expiration-seconds}")
    private long accessExpiration;

    @Value("${jwt.refresh-expiration-seconds}")
    private long refreshExpiration;


    public void addAuthCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        response.addHeader(HttpHeaders.SET_COOKIE, createCookie("accessToken", accessToken, accessExpiration).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, createCookie("refreshToken", refreshToken, refreshExpiration).toString());
    }

    /**
     * 로그아웃 시 만료된 쿠키발급
     */
    public void clearAuthCookies(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, createExpiredCookie("accessToken").toString());
        response.addHeader(HttpHeaders.SET_COOKIE, createExpiredCookie("refreshToken").toString());
        response.addHeader(HttpHeaders.SET_COOKIE, createExpiredCookie("XSRF-TOKEN").toString());
    }

    private ResponseCookie createCookie(String name, String value, long maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(maxAge)
                .sameSite("Lax")
                .build();
    }

    private ResponseCookie createExpiredCookie(String name) {
        return ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();
    }
}