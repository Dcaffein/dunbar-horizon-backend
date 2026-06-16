package com.example.DunbarHorizon.social.adapter.out.persistence.neo4j;

import com.example.DunbarHorizon.social.adapter.out.persistence.neo4j.springData.FriendshipNeo4jRepository;
import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.social.domain.friend.FriendshipArchiveCandidate;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendshipRepository;
import com.example.DunbarHorizon.social.domain.socialUser.UserReference;

import lombok.RequiredArgsConstructor;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Repository
@RequiredArgsConstructor
public class FriendshipRepositoryAdapter implements FriendshipRepository {

    private final FriendshipNeo4jRepository friendshipNeo4jRepository;
    private final Neo4jClient neo4jClient;

    @Override
    public List<Friendship> findFriendships(Long userId) {
        return friendshipNeo4jRepository.findFriendshipsByUserId(userId);
    }

    @Override
    public Friendship save(Friendship friendship) {
        return friendshipNeo4jRepository.save(friendship);
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
    public Set<Long> findFriendIds(Long userId) {
        return friendshipNeo4jRepository.findFriendIds(userId);
    }

    @Override
    public Set<UserReference> findFriends(Long userId) {
        List<Friendship> friendships = friendshipNeo4jRepository.findFriendshipsByUserId(userId);

        return friendships.stream()
                .map(friendship -> friendship.getFriend(userId))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Long> findFriendIdsIn(Long userId, Collection<Long> candidateIds) {
        return friendshipNeo4jRepository.findFriendIdsIn(userId, candidateIds);
    }

    @Override
    public Set<UserReference> findFriendsIn(Long userId, Collection<Long> targetIds) {
        List<Friendship> friendships = friendshipNeo4jRepository.findFriendshipsIn(userId, targetIds);

        return friendships.stream()
                .map(friendship -> friendship.getFriend(userId))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Long> findFriendIdsByMuteStatus(Long userId, boolean isMuted) {
        return friendshipNeo4jRepository.findFriendIdsByMuteStatus(userId, isMuted);
    }

    @Override
    public Set<UserReference> findFriendsByMuteStatus(Long userId, boolean isMuted) {
        List<Friendship> friendships = friendshipNeo4jRepository.findFriendshipsByMuteStatus(userId, isMuted);

        return friendships.stream()
                .map(friendship -> friendship.getFriend(userId))
                .collect(Collectors.toSet());
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
    public List<Friendship> findAllByIds(List<String> ids) {
        return StreamSupport.stream(
                friendshipNeo4jRepository.findAllById(ids).spliterator(), false
        ).collect(Collectors.toList());
    }

    @Override
    public void batchUpdateInterestScores(List<Map<String, Object>> updates, LocalDateTime lastInteractedAt) {
        friendshipNeo4jRepository.batchUpdateInterestScores(updates, lastInteractedAt);
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