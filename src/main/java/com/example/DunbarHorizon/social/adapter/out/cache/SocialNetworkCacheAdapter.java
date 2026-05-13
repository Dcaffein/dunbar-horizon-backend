package com.example.DunbarHorizon.social.adapter.out.cache;

import com.example.DunbarHorizon.social.application.dto.result.NetworkFriendEdgeResult;
import com.example.DunbarHorizon.social.application.port.out.SocialNetworkCacheRepository;
import com.example.DunbarHorizon.social.domain.friend.DunbarCircle;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class SocialNetworkCacheAdapter implements SocialNetworkCacheRepository {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final Duration TTL = Duration.ofMinutes(10);
    private static final String DEFAULT_KEY_PREFIX = "dunbar:network:default:";
    private static final String LABEL_KEY_PREFIX = "dunbar:network:label:";

    @Override
    @SuppressWarnings("unchecked")
    public Optional<List<NetworkFriendEdgeResult>> getDefaultNetwork(Long userId, DunbarCircle circleSize) {
        Object value = redisTemplate.opsForValue().get(defaultKey(userId, circleSize));
        return Optional.ofNullable((List<NetworkFriendEdgeResult>) value);
    }

    @Override
    public void putDefaultNetwork(Long userId, DunbarCircle circleSize, List<NetworkFriendEdgeResult> result) {
        redisTemplate.opsForValue().set(defaultKey(userId, circleSize), result, TTL);
    }

    @Override
    public void evictDefaultNetwork(Long userId) {
        deleteByPattern(DEFAULT_KEY_PREFIX + userId + ":*");
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<List<NetworkFriendEdgeResult>> getLabelNetwork(Long userId, String labelId) {
        Object value = redisTemplate.opsForValue().get(labelKey(userId, labelId));
        return Optional.ofNullable((List<NetworkFriendEdgeResult>) value);
    }

    @Override
    public void putLabelNetwork(Long userId, String labelId, List<NetworkFriendEdgeResult> result) {
        redisTemplate.opsForValue().set(labelKey(userId, labelId), result, TTL);
    }

    @Override
    public void evictLabelNetwork(Long userId, String labelId) {
        redisTemplate.delete(labelKey(userId, labelId));
    }

    @Override
    public void evictAllLabelNetworks(Long userId) {
        deleteByPattern(LABEL_KEY_PREFIX + userId + ":*");
    }

    private void deleteByPattern(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    private String defaultKey(Long userId, DunbarCircle circleSize) {
        return DEFAULT_KEY_PREFIX + userId + ":" + circleSize.name();
    }

    private String labelKey(Long userId, String labelId) {
        return LABEL_KEY_PREFIX + userId + ":" + labelId;
    }
}
