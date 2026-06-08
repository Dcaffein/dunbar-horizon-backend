package com.example.DunbarHorizon.social.adapter.out.redis;

import com.example.DunbarHorizon.social.application.port.out.SocialNetworkCacheManager;
import com.example.DunbarHorizon.social.domain.friend.DunbarCircle;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class SocialNetworkCacheAdapter implements SocialNetworkCacheManager {

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void evictDefaultNetwork(Long userId) {
        try {
            for (DunbarCircle circle : DunbarCircle.values()) {
                redisTemplate.delete("dunbar:network:default:" + userId + ":" + circle.name());
            }
        } catch (Exception e) {
            log.warn("Redis 캐시 무효화 실패 (default network, userId={}): {}", userId, e.getMessage());
        }
    }

    @Override
    public void evictLabelNetwork(Long userId, String labelId) {
        try {
            redisTemplate.delete("dunbar:network:label:" + userId + ":" + labelId);
        } catch (Exception e) {
            log.warn("Redis 캐시 무효화 실패 (label network, userId={}, labelId={}): {}", userId, labelId, e.getMessage());
        }
    }

    @Override
    public void evictAllLabelNetworks(Long userId) {
        try {
            Set<String> keys = redisTemplate.keys("dunbar:network:label:" + userId + ":*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.warn("Redis 캐시 무효화 실패 (all label networks, userId={}): {}", userId, e.getMessage());
        }
    }
}
