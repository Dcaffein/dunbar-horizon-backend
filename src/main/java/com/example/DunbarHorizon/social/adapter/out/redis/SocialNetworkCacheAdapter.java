package com.example.DunbarHorizon.social.adapter.out.redis;

import com.example.DunbarHorizon.social.application.port.out.SocialNetworkCacheManager;
import com.example.DunbarHorizon.social.domain.friend.DunbarCircle;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SocialNetworkCacheAdapter implements SocialNetworkCacheManager {

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void evictDefaultNetwork(Long userId) {
        try {
            redisTemplate.delete("dunbar:network:default:" + userId + ":" + DunbarCircle.DUNBAR.name());
        } catch (Exception e) {
            log.warn("Redis 캐시 무효화 실패 (default network, userId={}): {}", userId, e.getMessage());
        }
    }
}
