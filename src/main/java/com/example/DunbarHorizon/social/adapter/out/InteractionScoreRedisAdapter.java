package com.example.DunbarHorizon.social.adapter.out;

import com.example.DunbarHorizon.social.application.port.out.FriendshipDelta;
import com.example.DunbarHorizon.social.application.port.out.InteractionScoreDeltaPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class InteractionScoreRedisAdapter implements InteractionScoreDeltaPort {

    private static final String UNI_PREFIX    = "interaction:delta:uni:";
    private static final String MUTUAL_PREFIX = "interaction:delta:mutual:";

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void accumulate(String friendshipId, Long userId, double delta) {
        stringRedisTemplate.opsForHash().increment(UNI_PREFIX + friendshipId, String.valueOf(userId), delta);
    }

    @Override
    public void accumulateMutual(String friendshipId, double delta) {
        stringRedisTemplate.opsForValue().increment(MUTUAL_PREFIX + friendshipId, (long) delta);
    }

    @Override
    public Map<String, FriendshipDelta> drainAll() {
        Map<String, Map<Long, Double>> unilateralMap = drainUnilateral();
        Map<String, Double> mutualMap = drainMutual();

        Set<String> allIds = new java.util.HashSet<>();
        allIds.addAll(unilateralMap.keySet());
        allIds.addAll(mutualMap.keySet());

        Map<String, FriendshipDelta> result = new HashMap<>();
        for (String friendshipId : allIds) {
            Map<Long, Double> unilateral = unilateralMap.getOrDefault(friendshipId, Map.of());
            double mutual = mutualMap.getOrDefault(friendshipId, 0.0);
            result.put(friendshipId, new FriendshipDelta(unilateral, mutual));
        }
        return result;
    }

    private Map<String, Map<Long, Double>> drainUnilateral() {
        Set<String> keys = stringRedisTemplate.keys(UNI_PREFIX + "*");
        Map<String, Map<Long, Double>> result = new HashMap<>();
        if (keys == null || keys.isEmpty()) return result;

        for (String key : keys) {
            Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(key);
            stringRedisTemplate.delete(key);
            if (entries.isEmpty()) continue;

            String friendshipId = key.substring(UNI_PREFIX.length());
            Map<Long, Double> deltas = new HashMap<>();
            entries.forEach((k, v) -> deltas.put(Long.parseLong((String) k), Double.parseDouble((String) v)));
            result.put(friendshipId, deltas);
        }
        return result;
    }

    private Map<String, Double> drainMutual() {
        Set<String> keys = stringRedisTemplate.keys(MUTUAL_PREFIX + "*");
        Map<String, Double> result = new HashMap<>();
        if (keys == null || keys.isEmpty()) return result;

        for (String key : keys) {
            String value = stringRedisTemplate.opsForValue().getAndDelete(key);
            if (value == null) continue;

            String friendshipId = key.substring(MUTUAL_PREFIX.length());
            result.put(friendshipId, Double.parseDouble(value));
        }
        return result;
    }
}
