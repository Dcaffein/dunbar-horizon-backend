package com.example.DunbarHorizon.account.adapter.out.persistence;

import com.example.DunbarHorizon.account.domain.repository.EmailVerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class EmailVerificationTokenRedisAdapter implements EmailVerificationTokenRepository {

    private static final String TOKEN_KEY_PREFIX = "account:email-verification:";
    private static final String USER_KEY_PREFIX = "account:email-verification:user:";
    private static final Duration TTL = Duration.ofHours(24);

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void save(Long userId, String token) {
        stringRedisTemplate.opsForValue().set(tokenKey(token), String.valueOf(userId), TTL);
        stringRedisTemplate.opsForValue().set(userKey(userId), token, TTL);
    }

    @Override
    public Optional<Long> findUserIdByToken(String token) {
        String value = stringRedisTemplate.opsForValue().get(tokenKey(token));
        return Optional.ofNullable(value).map(Long::parseLong);
    }

    @Override
    public void deleteByUserId(Long userId) {
        String existingToken = stringRedisTemplate.opsForValue().get(userKey(userId));
        if (existingToken != null) {
            stringRedisTemplate.delete(tokenKey(existingToken));
        }
        stringRedisTemplate.delete(userKey(userId));
    }

    private String tokenKey(String token) {
        return TOKEN_KEY_PREFIX + token;
    }

    private String userKey(Long userId) {
        return USER_KEY_PREFIX + userId;
    }
}
