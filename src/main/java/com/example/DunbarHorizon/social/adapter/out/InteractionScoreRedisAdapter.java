package com.example.DunbarHorizon.social.adapter.out;

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

    private static final String KEY_PREFIX = "interaction:delta:";

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void accumulate(String friendshipId, Long userId, double delta) {
        stringRedisTemplate.opsForHash().increment(KEY_PREFIX + friendshipId, String.valueOf(userId), delta);
    }

    @Override
    public Map<String, Map<String, Double>> drainAll() {
        Set<String> keys = stringRedisTemplate.keys(KEY_PREFIX + "*");
        Map<String, Map<String, Double>> result = new HashMap<>();

        if (keys == null || keys.isEmpty()) {
            return result;
        }

        for (String key : keys) {
            Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(key);
            stringRedisTemplate.delete(key);

            if (entries.isEmpty()) continue;

            String friendshipId = key.substring(KEY_PREFIX.length());
            Map<String, Double> deltas = new HashMap<>();
            entries.forEach((k, v) -> deltas.put((String) k, Double.parseDouble((String) v)));
            result.put(friendshipId, deltas);
        }

        return result;
    }
}
