package com.example.DunbarHorizon.social.application.service;

import com.example.DunbarHorizon.global.annotation.Neo4jTransactional;
import com.example.DunbarHorizon.social.application.port.out.InteractionScoreDeltaPort;
import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendshipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class InteractionScoreFlushService {

    private final InteractionScoreDeltaPort deltaPort;
    private final FriendshipRepository friendshipRepository;

    @Neo4jTransactional
    public void flush() {
        Map<String, Map<String, Double>> deltas = deltaPort.drainAll();
        if (deltas.isEmpty()) return;

        List<String> friendshipIds = new ArrayList<>(deltas.keySet());
        List<Friendship> friendships = friendshipRepository.findAllByIds(friendshipIds);

        if (friendships.isEmpty()) return;

        LocalDateTime now = LocalDateTime.now();
        List<Map<String, Object>> updates = new ArrayList<>();

        for (Friendship friendship : friendships) {
            Map<String, Double> friendshipDeltas = deltas.get(friendship.getId());
            if (friendshipDeltas == null) continue;

            friendshipDeltas.forEach((userIdStr, delta) ->
                    friendship.adjustInterestScore(Long.parseLong(userIdStr), delta)
            );

            friendshipDeltas.keySet().forEach(userIdStr -> {
                Long userId = Long.parseLong(userIdStr);
                updates.add(Map.of(
                        "friendshipId", friendship.getId(),
                        "userId", userId,
                        "interestScore", friendship.getMyInterestScore(userId),
                        "intimacy", friendship.getIntimacy()
                ));
            });
        }

        if (!updates.isEmpty()) {
            friendshipRepository.batchUpdateInterestScores(updates, now);
            log.debug("Flushed interaction scores: {} updates", updates.size());
        }
    }
}
