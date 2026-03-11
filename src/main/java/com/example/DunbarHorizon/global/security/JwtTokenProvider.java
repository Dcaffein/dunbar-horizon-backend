package com.example.DunbarHorizon.global.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {

    private final Key key;
    private final long accessTokenValidityInMilliseconds;
    private final long refreshTokenValidityInMilliseconds;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-expiration-seconds}") long accessExpirationSeconds,
            @Value("${jwt.refresh-expiration-seconds}") long refreshExpirationSeconds) {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenValidityInMilliseconds = accessExpirationSeconds * 1000;
        this.refreshTokenValidityInMilliseconds = refreshExpirationSeconds * 1000;
    }

    public String createAccessToken(AuthPrincipal principal) {
        return createToken(principal, accessTokenValidityInMilliseconds);
    }

    public String createRefreshToken(AuthPrincipal principal) {
        return createToken(principal, refreshTokenValidityInMilliseconds);
    }

    private String createToken(AuthPrincipal principal, long validity) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + validity);

        return Jwts.builder()
                .setSubject(principal.id().toString())
                .claim("role", principal.role())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    public AuthPrincipal validateToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            Long userId = Long.parseLong(claims.getSubject());
            String role = claims.get("role", String.class);

            return new AuthPrincipal(userId, role);

        } catch (SecurityException | MalformedJwtException ex) {
            log.error("잘못된 JWT 서명입니다.");
            throw ex;
        } catch (ExpiredJwtException ex) {
            log.error("만료된 JWT 토큰입니다.");
            throw ex;
        } catch (UnsupportedJwtException ex) {
            log.error("지원되지 않는 JWT 토큰입니다.");
            throw ex;
        } catch (IllegalArgumentException ex) {
            log.error("JWT 토큰이 비어있습니다.");
            throw ex;
        }
    }

    public LocalDateTime getExpirationTime(String token) {
        Date expiration = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getExpiration();

        return expiration.toInstant()
                .atZone(ZoneOffset.UTC)
                .toLocalDateTime();
    }
}
