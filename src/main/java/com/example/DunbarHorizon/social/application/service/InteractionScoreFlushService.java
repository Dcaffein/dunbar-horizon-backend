package com.example.DunbarHorizon.social.application.service;

import com.example.DunbarHorizon.global.annotation.Neo4jTransactional;
import com.example.DunbarHorizon.social.application.port.out.FriendshipDelta;
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
        Map<String, FriendshipDelta> deltas = deltaPort.drainAll();
        if (deltas.isEmpty()) return;

        List<Friendship> friendships = friendshipRepository.findAllByIds(new ArrayList<>(deltas.keySet()));
        if (friendships.isEmpty()) return;

        LocalDateTime now = LocalDateTime.now();
        List<Map<String, Object>> updates = new ArrayList<>();

        for (Friendship friendship : friendships) {
            FriendshipDelta delta = deltas.get(friendship.getId());
            if (delta == null) continue;

            delta.unilateral().forEach((userId, d) ->
                    friendship.adjustInterestScore(userId, d)
            );

            if (delta.hasMutual()) {
                friendship.adjustMutualInterestScore(delta.mutual());
            }

            delta.unilateral().keySet().forEach(userId -> updates.add(Map.of(
                    "friendshipId", friendship.getId(),
                    "userId", userId,
                    "interestScore", friendship.getMyInterestScore(userId),
                    "intimacy", friendship.getIntimacy()
            )));

            if (delta.hasMutual()) {
                friendship.getUsers().forEach(user -> updates.add(Map.of(
                        "friendshipId", friendship.getId(),
                        "userId", user.getId(),
                        "interestScore", friendship.getMyInterestScore(user.getId()),
                        "intimacy", friendship.getIntimacy()
                )));
            }
        }

        if (!updates.isEmpty()) {
            friendshipRepository.batchUpdateInterestScores(updates, now);
            log.debug("Flushed interaction scores: {} updates", updates.size());
        }
    }
}
