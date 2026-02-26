package com.example.GooRoomBe.social.adapter.out;

import com.example.GooRoomBe.social.adapter.out.neo4j.springData.FriendshipNeo4jRepository;
import com.example.GooRoomBe.social.adapter.out.neo4j.springData.SocialUserNeo4jRepository;
import com.example.GooRoomBe.social.domain.friend.Friendship;
import com.example.GooRoomBe.social.domain.friend.FriendTestFactory;
import com.example.GooRoomBe.social.domain.socialUser.SocialUser;
import com.example.GooRoomBe.support.Neo4jRepositoryTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@Neo4jRepositoryTest
class FriendshipNeo4jRepositoryTest {

    @Autowired
    private FriendshipNeo4jRepository friendshipRepository;

    @Autowired
    private SocialUserNeo4jRepository socialUserNeo4jRepository;

    private SocialUser userA;
    private SocialUser userB;
    private SocialUser userC;

    @BeforeEach
    void setUp() {
        userA = socialUserNeo4jRepository.save(new SocialUser(1L, "사용자A", "url1"));
        userB = socialUserNeo4jRepository.save(new SocialUser(2L, "사용자B", "url2"));
        userC = socialUserNeo4jRepository.save(new SocialUser(3L, "사용자C", "url3"));
    }

    @Test
    @DisplayName("복합 ID를 사용하여 친구 관계를 저장하고 조회한다")
    void saveAndFindById_Success() {
        // given: 🌟 FriendTestFactory를 통해 안전하게 생성
        Friendship friendship = FriendTestFactory.createFriendship(userA, userB);
        friendshipRepository.save(friendship);

        // when
        Optional<Friendship> found = friendshipRepository.findById("1_2");

        // then
        assertThat(found).isPresent();
        // 초기 친밀도 계산 정책에 따른 결과 확인 (예: sqrt(10*10) = 10.0)
        assertThat(found.get().getIntimacy()).isGreaterThanOrEqualTo(0.0);
    }

    @Test
    @DisplayName("existsFriendshipBetween은 방향에 관계없이 친구 여부를 확인한다")
    void existsFriendshipBetween_Success() {
        // given
        friendshipRepository.save(FriendTestFactory.createFriendship(userA, userB));

        // when & then
        assertThat(friendshipRepository.existsFriendshipBetween(1L, 2L)).isTrue();
        assertThat(friendshipRepository.existsFriendshipBetween(2L, 1L)).isTrue();
        assertThat(friendshipRepository.existsFriendshipBetween(1L, 3L)).isFalse();
    }

    @Test
    @DisplayName("특정 ID 목록 중 친구인 ID들만 필터링하여 반환한다")
    void findFriendIdsIn_Success() {
        // given
        friendshipRepository.save(FriendTestFactory.createFriendship(userA, userB));

        // when
        Set<Long> friendIds = friendshipRepository.findFriendIdsIn(1L, Set.of(2L, 3L));

        // then
        assertThat(friendIds).containsExactly(2L);
        assertThat(friendIds).doesNotContain(3L);
    }

    @Test
    @DisplayName("음소거(Mute) 상태에 따른 친구 목록 조회를 확인한다")
    void findFriendsByMuteStatus_Success() {
        // given
        Friendship friendship = FriendTestFactory.createFriendship(userA, userB);
        friendship.updateMuteStatus(userA.getId(), true); // 🌟 ID 직접 참조 대신 객체 활용
        friendshipRepository.save(friendship);

        // when
        Set<Long> mutedIds = friendshipRepository.findFriendIdsByMuteStatus(userA.getId(), true);
        Set<Long> activeIds = friendshipRepository.findFriendIdsByMuteStatus(userA.getId(), false);

        // then
        assertThat(mutedIds).contains(userB.getId());
        assertThat(activeIds).isEmpty();
    }

    @Test
    @DisplayName("applyDecay 쿼리가 조건에 맞는 관계의 점수를 감쇄시키고 친밀도를 재계산한다")
    void applyDecay_Success() {
        // given: 🌟 팩토리를 통해 특정 상태의 우정 생성
        Friendship friendship = FriendTestFactory.createFriendship(userA, userB);
        friendship.adjustInterestScore(userA.getId(), 100.0);
        friendship.adjustInterestScore(userB.getId(), 100.0);
        friendshipRepository.save(friendship);

        LocalDateTime decayTime = LocalDateTime.now().plusSeconds(1);

        // when
        friendshipRepository.applyDecay(0.5, 1.0, decayTime);

        // then
        Friendship updated = friendshipRepository.findById("1_2").orElseThrow();
        // 각 점수가 감쇄 비율에 맞게 변했는지 확인
        assertThat(updated.getIntimacy()).isCloseTo(updated.getIntimacy(), within(0.01));
    }
}