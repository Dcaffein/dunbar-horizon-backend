package com.example.DunbarHorizon.social.adapter.out;

import com.example.DunbarHorizon.social.application.port.out.SocialNetworkCacheManager;
import com.example.DunbarHorizon.social.domain.friend.DunbarCircle;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class SocialNetworkCacheAdapter implements SocialNetworkCacheManager {

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void evictDefaultNetwork(Long userId) {
        for (DunbarCircle circle : DunbarCircle.values()) {
            redisTemplate.delete("dunbar:network:default:" + userId + ":" + circle.name());
        }
    }

    @Override
    public void evictLabelNetwork(Long userId, String labelId) {
        redisTemplate.delete("dunbar:network:label:" + userId + ":" + labelId);
    }

    @Override
    public void evictAllLabelNetworks(Long userId) {
        Set<String> keys = redisTemplate.keys("dunbar:network:label:" + userId + ":*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}
