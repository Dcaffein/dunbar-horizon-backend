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


    public void addAuthCookies(HttpServletResponse response, String accessToken, String refreshToken, String csrfToken) {
        response.addHeader(HttpHeaders.SET_COOKIE, createCookie("accessToken", accessToken, accessExpiration, true).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, createCookie("refreshToken", refreshToken, refreshExpiration, true).toString());

        if (csrfToken != null) {
            response.addHeader(HttpHeaders.SET_COOKIE, createCookie("XSRF-TOKEN", csrfToken, accessExpiration, false).toString());
        }
    }

    /**
     * 로그아웃 시 만료된 쿠키발급
     */
    public void clearAuthCookies(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, createExpiredCookie("accessToken").toString());
        response.addHeader(HttpHeaders.SET_COOKIE, createExpiredCookie("refreshToken").toString());
        response.addHeader(HttpHeaders.SET_COOKIE, createExpiredCookie("XSRF-TOKEN").toString());
    }

    private ResponseCookie createCookie(String name, String value, long maxAge, boolean httpOnly) {
        return ResponseCookie.from(name, value)
                .httpOnly(httpOnly)
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