package com.example.DunbarHorizon.social.adapter.out;

import com.example.DunbarHorizon.social.adapter.out.neo4j.springData.FriendshipNeo4jRepository;
import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendshipRepository;
import com.example.DunbarHorizon.social.domain.socialUser.UserReference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class FriendshipRepositoryAdapter implements FriendshipRepository {

    private final FriendshipNeo4jRepository friendshipNeo4jRepository;

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
    public void delete(Friendship friendship) {
        friendshipNeo4jRepository.delete(friendship);
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
}