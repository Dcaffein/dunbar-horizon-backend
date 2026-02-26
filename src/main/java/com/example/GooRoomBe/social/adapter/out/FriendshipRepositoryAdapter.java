package com.example.GooRoomBe.social.adapter.out;

import com.example.GooRoomBe.social.adapter.out.neo4j.springData.FriendshipNeo4jRepository;
import com.example.GooRoomBe.social.domain.friend.Friendship;
import com.example.GooRoomBe.social.domain.friend.repository.FriendshipRepository;
import com.example.GooRoomBe.social.domain.socialUser.UserReference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class FriendshipRepositoryAdapter implements FriendshipRepository {

    private final FriendshipNeo4jRepository friendshipNeo4jRepository;

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
        return friendshipNeo4jRepository.findFriends(userId);
    }

    @Override
    public Set<Long> findFriendIdsIn(Long userId, Collection<Long> candidateIds) {
        return friendshipNeo4jRepository.findFriendIdsIn(userId, candidateIds);
    }

    @Override
    public Set<UserReference> findFriendsIn(Long userId, Collection<Long> targetIds) {
        return friendshipNeo4jRepository.findFriendsIn(userId, targetIds);
    }

    @Override
    public Set<Long> findFriendIdsByMuteStatus(Long userId, boolean isMuted) {
        return friendshipNeo4jRepository.findFriendIdsByMuteStatus(userId, isMuted);
    }

    @Override
    public Set<UserReference> findFriendsByMuteStatus(Long userId, boolean isMuted) {
        return friendshipNeo4jRepository.findFriendsByMuteStatus(userId, isMuted);
    }

    @Override
    public void applyDecay(double rate, double threshold, LocalDateTime decayTime) {
        friendshipNeo4jRepository.applyDecay(rate, threshold, decayTime);
    }
}
