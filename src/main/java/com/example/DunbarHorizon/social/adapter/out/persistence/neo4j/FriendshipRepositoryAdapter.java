package com.example.DunbarHorizon.social.adapter.out.persistence.neo4j;

import com.example.DunbarHorizon.social.adapter.out.persistence.neo4j.springData.FriendshipNeo4jRepository;
import com.example.DunbarHorizon.social.domain.friend.FriendRecognition;
import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.social.domain.friend.FriendshipArchiveCandidate;
import com.example.DunbarHorizon.social.domain.friend.MutualInterestScoreUpdate;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendshipRepository;
import com.example.DunbarHorizon.social.domain.socialUser.SocialUser;
import com.example.DunbarHorizon.social.domain.socialUser.exception.UserReferenceNotFoundException;

import lombok.RequiredArgsConstructor;
import org.neo4j.driver.Record;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.example.DunbarHorizon.social.adapter.out.persistence.neo4j.schema.SocialGraphSchema.*;
import static com.example.DunbarHorizon.social.domain.socialUser.constant.SocialUserConstants.USER_REFERENCE;

@Repository
@RequiredArgsConstructor
public class FriendshipRepositoryAdapter implements FriendshipRepository {

    private final FriendshipNeo4jRepository friendshipNeo4jRepository;
    private final Neo4jClient neo4jClient;

    private static final String CREATE_FRIENDSHIP =
            "OPTIONAL MATCH (a:" + USER_REFERENCE + " {id: $userAId}) " +
            "OPTIONAL MATCH (b:" + USER_REFERENCE + " {id: $userBId}) " +
            "CALL { " +
            "  WITH a, b " +
            "  WITH a, b WHERE a IS NOT NULL AND b IS NOT NULL " +
            "  MERGE (a)-[r:" + FRIENDS_WITH + " {id: $friendshipId}]->(b) ON CREATE SET r += $properties " +
            "  RETURN count(r) AS created " +
            "} " +
            "RETURN a IS NULL AS missingA, b IS NULL AS missingB";

    private static final String FIND_BY_ID =
            "MATCH (low:" + USER_REFERENCE + ")-[r:" + FRIENDS_WITH + " {id: $friendshipId}]->(high:" + USER_REFERENCE + ") " +
            "RETURN r, low, high";

    private static final String FIND_BY_USER_ID =
            "MATCH (:" + USER_REFERENCE + " {id: $userId})-[r:" + FRIENDS_WITH + "]-(:" + USER_REFERENCE + ") " +
            "RETURN r, startNode(r) AS low, endNode(r) AS high";

    private static final String FIND_ARCHIVE_CANDIDATES =
            "MATCH (u:" + USER_REFERENCE + ")-[r:" + FRIENDS_WITH + "]->(v:" + USER_REFERENCE + ") " +
            "WHERE r." + PROP_INTIMACY + " <= $threshold " +
            "RETURN r." + PROP_ID + " AS id, u." + PROP_ID + " AS userAId, v." + PROP_ID + " AS userBId, " +
            "r." + PROP_CREATED_AT + " AS friendedAt";

    @Override
    public void create(Friendship friendship) {
        List<FriendRecognition> recognitions = friendship.getRecognitions().stream()
                .sorted(Comparator.comparing(recognition -> recognition.getUser().getId()))
                .toList();
        FriendRecognition low = recognitions.get(0);
        FriendRecognition high = recognitions.get(1);
        Long userAId = low.getUser().getId();
        Long userBId = high.getUser().getId();

        Map<String, Object> properties = new HashMap<>();
        properties.put(PROP_CREATED_AT, friendship.getCreatedAt());
        properties.put(PROP_INTIMACY, friendship.getIntimacy());
        properties.putAll(propertiesOf(SIDE_LOW, low));
        properties.putAll(propertiesOf(SIDE_HIGH, high));

        Map<String, Object> result = neo4jClient.query(CREATE_FRIENDSHIP)
                .bindAll(Map.of(
                        "friendshipId", friendship.getId(),
                        "userAId", userAId,
                        "userBId", userBId,
                        "properties", properties
                ))
                .fetch()
                .one()
                .orElseThrow();

        if ((Boolean) result.get("missingA")) throw new UserReferenceNotFoundException(userAId);
        if ((Boolean) result.get("missingB")) throw new UserReferenceNotFoundException(userBId);
    }

    private Map<String, Object> propertiesOf(String side, FriendRecognition recognition) {
        Map<String, Object> properties = new HashMap<>();
        if (!recognition.getFriendAlias().isEmpty()) {
            properties.put(side + SUFFIX_FRIEND_ALIAS, recognition.getFriendAlias());
        }
        properties.put(side + SUFFIX_INTEREST_SCORE, recognition.getInterestScore());
        properties.put(side + SUFFIX_LAST_INTERACTED_AT, recognition.getLastInteractedAt());
        properties.put(side + SUFFIX_IS_MUTED, recognition.isMuted());
        properties.put(side + SUFFIX_IS_ROUTABLE, recognition.isRoutable());
        return properties;
    }

    private Friendship toFriendship(Record record) {
        Relationship relationship = record.get("r").asRelationship();
        return Friendship.restore(
                relationship.get(PROP_ID).asString(),
                relationship.get(PROP_CREATED_AT).asLocalDate(null),
                relationship.get(PROP_INTIMACY).asDouble(0.0),
                List.of(
                        toRecognition(SIDE_LOW, record.get("low").asNode(), relationship),
                        toRecognition(SIDE_HIGH, record.get("high").asNode(), relationship)
                )
        );
    }

    private FriendRecognition toRecognition(String side, Node user, Relationship relationship) {
        return FriendRecognition.restore(
                new SocialUser(
                        user.get(PROP_ID).asLong(),
                        user.get(PROP_NICKNAME).asString(null),
                        user.get(PROP_PROFILE_IMAGE_URL).asString(null)
                ),
                relationship.get(side + SUFFIX_FRIEND_ALIAS).asString(null),
                relationship.get(side + SUFFIX_INTEREST_SCORE).asDouble(0.0),
                relationship.get(side + SUFFIX_LAST_INTERACTED_AT).asLocalDateTime(null),
                relationship.get(side + SUFFIX_IS_MUTED).asBoolean(false),
                relationship.get(side + SUFFIX_IS_ROUTABLE).asBoolean(true)
        );
    }

    private static String sideOf(Long userId, Long friendId) {
        return userId < friendId ? SIDE_LOW : SIDE_HIGH;
    }

    @Override
    public Optional<Friendship> findById(String id) {
        return neo4jClient.query(FIND_BY_ID)
                .bind(id).to("friendshipId")
                .fetchAs(Friendship.class)
                .mappedBy((typeSystem, record) -> toFriendship(record))
                .one();
    }

    @Override
    public void delete(String friendshipId) {
        friendshipNeo4jRepository.deleteFriendship(friendshipId);
    }

    @Override
    public boolean existsFriendshipBetween(Long userId, Long targetId) {
        return friendshipNeo4jRepository.existsFriendship(Friendship.generateCompositeId(userId, targetId));
    }

    @Override
    public List<Friendship> findByUserId(Long userId) {
        return neo4jClient.query(FIND_BY_USER_ID)
                .bind(userId).to("userId")
                .fetchAs(Friendship.class)
                .mappedBy((typeSystem, record) -> toFriendship(record))
                .all()
                .stream()
                .toList();
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
        String side = sideOf(userId, friendship.getFriend(userId).getId());
        friendshipNeo4jRepository.updateUserRelationshipFields(
                friendship.getId(),
                Map.of(
                        side + SUFFIX_FRIEND_ALIAS, friendship.getFriendAlias(userId),
                        side + SUFFIX_IS_MUTED, friendship.isMuted(userId),
                        side + SUFFIX_IS_ROUTABLE, friendship.isRoutable(userId)
                )
        );
    }

    @Override
    public void incrementInterestScore(String friendshipId, Long userId, Long friendId, double delta) {
        friendshipNeo4jRepository.incrementInterestScore(friendshipId, userId, delta);
    }

    @Override
    public void incrementMutualInterestScore(String friendshipId, Long userAId, Long userBId, double delta) {
        friendshipNeo4jRepository.incrementMutualInterestScore(friendshipId, delta);
    }

    @Override
    public void incrementMutualInterestScoresBatch(List<MutualInterestScoreUpdate> updates) {
        if (updates.isEmpty()) return;
        friendshipNeo4jRepository.incrementMutualInterestScoresBatch(
                updates.stream()
                        .map(update -> Map.<String, Object>of(
                                "friendshipId", update.friendshipId(),
                                "delta", update.delta()
                        ))
                        .toList()
        );
    }

    @Override
    public List<FriendshipArchiveCandidate> findArchiveCandidates(double threshold) {
        return neo4jClient.query(FIND_ARCHIVE_CANDIDATES)
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
        friendshipNeo4jRepository.deleteFriendships(ids);
    }
}
