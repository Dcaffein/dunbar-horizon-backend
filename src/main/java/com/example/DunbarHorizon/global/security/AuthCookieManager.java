package com.example.DunbarHorizon.global.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;

@Component
public class AuthCookieManager {

    @Value("${jwt.access-expiration-seconds}")
    private long accessExpiration;

    @Value("${jwt.refresh-expiration-seconds}")
    private long refreshExpiration;

    @Value("${app.cookie.domain:}")
    private String cookieDomain;

    @Value("${app.cookie.secure:false}")
    private boolean cookieSecure;

    public void addAccessTokenCookie(HttpServletResponse response, String accessToken) {
        response.addHeader(HttpHeaders.SET_COOKIE, createCookie("access_token", accessToken, accessExpiration).toString());
    }

    public void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        response.addHeader(HttpHeaders.SET_COOKIE, createCookie("refresh_token", refreshToken, refreshExpiration).toString());
    }

    public void addExpiredTokenCookie(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, createExpiredCookie("access_token").toString());
        response.addHeader(HttpHeaders.SET_COOKIE, createExpiredCookie("refresh_token").toString());
    }

    private ResponseCookie createCookie(String name, String value, long maxAge) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(maxAge)
                .sameSite("Lax");
        if (StringUtils.hasText(cookieDomain)) {
            builder.domain(cookieDomain);
        }
        return builder.build();
    }

    private ResponseCookie createExpiredCookie(String name) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(0)
                .sameSite("Lax");
        if (StringUtils.hasText(cookieDomain)) {
            builder.domain(cookieDomain);
        }
        return builder.build();
    }

    public String extractAccessToken(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(cookie -> "access_token".equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}
