package com.example.DunbarHorizon.social.adapter.out.persistence.neo4j;

import com.example.DunbarHorizon.social.adapter.out.persistence.neo4j.springData.FriendshipNeo4jRepository;
import com.example.DunbarHorizon.social.domain.friend.FriendRecognition;
import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.social.domain.friend.FriendshipArchiveCandidate;
import com.example.DunbarHorizon.social.domain.friend.MutualInterestScoreUpdate;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendshipRepository;
import com.example.DunbarHorizon.social.domain.socialUser.exception.UserReferenceNotFoundException;

import lombok.RequiredArgsConstructor;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.mapping.Constants;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.example.DunbarHorizon.social.domain.friend.constant.FriendConstants.FRIENDSHIP;
import static com.example.DunbarHorizon.social.domain.friend.constant.FriendConstants.HAS_FRIENDSHIP;
import static com.example.DunbarHorizon.social.domain.socialUser.constant.SocialUserConstants.USER_REFERENCE;

@Repository
@RequiredArgsConstructor
public class FriendshipRepositoryAdapter implements FriendshipRepository {

    private final FriendshipNeo4jRepository friendshipNeo4jRepository;
    private final Neo4jClient neo4jClient;
    private final Neo4jMappingContext mappingContext;

    private static final String CREATE_FRIENDSHIP =
            "OPTIONAL MATCH (a:" + USER_REFERENCE + " {id: $userAId}) " +
            "OPTIONAL MATCH (b:" + USER_REFERENCE + " {id: $userBId}) " +
            "CALL { " +
            "  WITH a, b " +
            "  WITH a, b WHERE a IS NOT NULL AND b IS NOT NULL " +
            "  MERGE (f:" + FRIENDSHIP + " {id: $friendshipId}) ON CREATE SET f += $friendship " +
            "  MERGE (a)-[ra:" + HAS_FRIENDSHIP + "]->(f) ON CREATE SET ra += $recognitionA " +
            "  MERGE (b)-[rb:" + HAS_FRIENDSHIP + "]->(f) ON CREATE SET rb += $recognitionB " +
            "  RETURN count(f) AS created " +
            "} " +
            "RETURN a IS NULL AS missingA, b IS NULL AS missingB";

    @Override
    public void create(Friendship friendship) {
        List<FriendRecognition> recognitions = friendship.getRecognitions().stream()
                .sorted(Comparator.comparing(recognition -> recognition.getUser().getId()))
                .toList();
        FriendRecognition recognitionA = recognitions.get(0);
        FriendRecognition recognitionB = recognitions.get(1);
        Long userAId = recognitionA.getUser().getId();
        Long userBId = recognitionB.getUser().getId();

        Map<String, Object> params = new HashMap<>();
        params.put("friendshipId", friendship.getId());
        params.put("friendship", propertiesOf(Friendship.class, friendship));
        params.put("userAId", userAId);
        params.put("userBId", userBId);
        params.put("recognitionA", propertiesOf(FriendRecognition.class, recognitionA));
        params.put("recognitionB", propertiesOf(FriendRecognition.class, recognitionB));

        Map<String, Object> result = neo4jClient.query(CREATE_FRIENDSHIP)
                .bindAll(params)
                .fetch()
                .one()
                .orElseThrow();

        if ((Boolean) result.get("missingA")) throw new UserReferenceNotFoundException(userAId);
        if ((Boolean) result.get("missingB")) throw new UserReferenceNotFoundException(userBId);
    }

    /**
     * SDN 매핑이 정한 엔티티 자신의 속성만 꺼낸다. 관계와 연결 노드는 포함하지 않는다.
     */
    @SuppressWarnings("unchecked")
    private <T> Map<String, Object> propertiesOf(Class<T> type, T entity) {
        return (Map<String, Object>) mappingContext.getRequiredBinderFunctionFor(type)
                .apply(entity)
                .get(Constants.NAME_OF_PROPERTIES_PARAM);
    }

    @Override
    public Optional<Friendship> findById(String id) {
        return friendshipNeo4jRepository.findById(id);
    }

    @Override
    public void delete(String friendshipId) {
        friendshipNeo4jRepository.deleteById(friendshipId);
    }

    @Override
    public boolean existsFriendshipBetween(Long userId, Long targetId) {
        return friendshipNeo4jRepository.existsFriendshipBetween(userId, targetId);
    }

    @Override
    public List<Friendship> findByUserId(Long userId) {
        return friendshipNeo4jRepository.findByUserId(userId);
    }

    @Override
    public Set<Long> findFriendIdsByMuteStatus(Long userId, boolean isMuted) {
        return friendshipNeo4jRepository.findFriendIdsByMuteStatus(userId, isMuted);
    }

    @Override
    public Set<Long> filterFriendIdsAmong(Long userId, Collection<Long> candidateIds) {
        return friendshipNeo4jRepository.filterFriendIdsAmong(userId, candidateIds);
    }

    @Override
    public void applyDecay(double rate, double threshold, LocalDateTime decayTime) {
        friendshipNeo4jRepository.applyDecay(rate, threshold, decayTime);
    }

    @Override
    public void updateUserFields(Friendship friendship, Long userId) {
        friendshipNeo4jRepository.updateUserRelationshipFields(
                friendship.getId(),
                userId,
                friendship.getFriendAlias(userId),
                friendship.isMuted(userId),
                friendship.isRoutable(userId)
        );
    }

    @Override
    public void incrementInterestScore(String friendshipId, Long userId, Long friendId, double delta) {
        friendshipNeo4jRepository.incrementInterestScore(friendshipId, userId, friendId, delta);
    }

    @Override
    public void incrementMutualInterestScore(String friendshipId, Long userAId, Long userBId, double delta) {
        friendshipNeo4jRepository.incrementMutualInterestScore(friendshipId, userAId, userBId, delta);
    }

    @Override
    public void incrementMutualInterestScoresBatch(List<MutualInterestScoreUpdate> updates) {
        if (updates.isEmpty()) return;
        friendshipNeo4jRepository.incrementMutualInterestScoresBatch(
                updates.stream()
                        .map(update -> Map.<String, Object>of(
                                "friendshipId", update.friendshipId(),
                                "userAId", update.userAId(),
                                "userBId", update.userBId(),
                                "delta", update.delta()
                        ))
                        .toList()
        );
    }

    @Override
    public List<FriendshipArchiveCandidate> findArchiveCandidates(double threshold) {
        return neo4jClient.query(
                "MATCH (u:UserReference)-[:HAS_FRIENDSHIP]->(f:Friendship)<-[:HAS_FRIENDSHIP]-(v:UserReference) " +
                "WHERE f.intimacy <= $threshold AND u.id < v.id " +
                "RETURN f.id AS id, u.id AS userAId, v.id AS userBId, f.createdAt AS friendedAt"
        )
                .bind(threshold).to("threshold")
                .fetchAs(FriendshipArchiveCandidate.class)
                .mappedBy((typeSystem, record) -> new FriendshipArchiveCandidate(
                        record.get("id").asString(),
                        record.get("userAId").asLong(),
                        record.get("userBId").asLong(),
                        record.get("friendedAt").isNull() ? null
                                : record.get("friendedAt").asLocalDate()
                ))
                .all()
                .stream()
                .toList();
    }

    @Override
    public void deleteAllByIds(Collection<String> ids) {
        if (ids.isEmpty()) return;
        friendshipNeo4jRepository.deleteAllById(ids);
    }
}
