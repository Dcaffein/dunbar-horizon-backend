package com.example.DunbarHorizon.social.adapter.out;

import com.example.DunbarHorizon.social.adapter.out.persistence.neo4j.springData.FriendshipNeo4jRepository;
import com.example.DunbarHorizon.social.adapter.out.persistence.neo4j.springData.SocialUserNeo4jRepository;
import com.example.DunbarHorizon.social.domain.friend.FriendRecognition;
import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.social.domain.friend.FriendTestFactory;
import com.example.DunbarHorizon.social.domain.socialUser.SocialUser;
import com.example.DunbarHorizon.support.Neo4jRepositoryTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.core.Neo4jClient;

import java.time.LocalDateTime;
import java.util.List;
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

    @Autowired
    private Neo4jClient neo4jClient;

    private SocialUser userA;
    private SocialUser userB;
    private SocialUser userC;

    @BeforeEach
    void setUp() {
        neo4jClient.query("MATCH (n) DETACH DELETE n").run();
        userA = socialUserNeo4jRepository.save(new SocialUser(1L, "사용자A", "url1"));
        userB = socialUserNeo4jRepository.save(new SocialUser(2L, "사용자B", "url2"));
        userC = socialUserNeo4jRepository.save(new SocialUser(3L, "사용자C", "url3"));
    }

    @Test
    @DisplayName("복합 ID를 사용하여 친구 관계를 저장하고 조회한다")
    void saveAndFindById_Success() {
        Friendship friendship = FriendTestFactory.createFriendship(userA, userB);
        friendshipRepository.save(friendship);

        // when
        Optional<Friendship> found = friendshipRepository.findById("1_2");

        // then
        assertThat(found).isPresent();
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
    @DisplayName("음소거(Mute) 상태에 따른 친구 ID 목록 조회를 확인한다")
    void findFriendIdsByMuteStatus_Success() {
        // given
        Friendship friendship = FriendTestFactory.createFriendship(userA, userB);
        friendship.updateMuteStatus(userA.getId(), true);
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
        // given
        Friendship friendship = FriendTestFactory.createFriendship(userA, userB);
        friendship.adjustInterestScore(userA.getId(), 100.0);
        friendship.adjustInterestScore(userB.getId(), 100.0);
        friendshipRepository.save(friendship);

        double rate = 0.5;
        double threshold = 1.0;
        double k = FriendRecognition.CONVERGENCE_K;
        double decayedScore = (FriendRecognition.INITIAL_RAW_SCORE + 100.0) * rate;
        double norm = decayedScore / (decayedScore + k);
        double expectedIntimacy = Math.sqrt(norm * norm);

        // when
        friendshipRepository.applyDecay(rate, threshold, LocalDateTime.now().plusSeconds(1));

        // then
        Friendship updated = friendshipRepository.findById("1_2").orElseThrow();
        assertThat(updated.getIntimacy()).isCloseTo(expectedIntimacy, within(0.0001));
    }

    @Test
    @DisplayName("한쪽만 30일 이상 교류가 없을 때 applyDecay는 intimacy를 0으로 덮어쓰지 않는다")
    void applyDecay_asymmetric_doesNotZeroIntimacy() {
        // given
        Friendship friendship = FriendTestFactory.createFriendship(userA, userB);
        friendshipRepository.save(friendship);

        // userA의 엣지만 60일 전으로 설정 → decayTime(30일 전) 조건 충족
        // userB의 엣지는 생성 직후 → decayTime 조건 미충족 (최신)
        neo4jClient.query(
                "MATCH (:UserReference {id: $userId})-[r:HAS_FRIENDSHIP]->(f:Friendship {id: $fid}) " +
                "SET r.lastInteractedAt = $past"
        ).bind(userA.getId()).to("userId")
         .bind("1_2").to("fid")
         .bind(LocalDateTime.now().minusDays(60)).to("past")
         .run();

        // when
        friendshipRepository.applyDecay(0.967, 1.0, LocalDateTime.now().minusDays(30));

        // then — 버그 시 intimacy = 0.0, 수정 후 양쪽 점수 기반 정상 계산값 > 0
        Friendship updated = friendshipRepository.findById("1_2").orElseThrow();
        assertThat(updated.getIntimacy()).isGreaterThan(0.0);
    }

    @Test
    @DisplayName("친구 관계를 삭제하면 더 이상 조회되지 않는다")
    void deleteById_Success() {
        // given
        Friendship friendship = FriendTestFactory.createFriendship(userA, userB);
        friendshipRepository.save(friendship);
        assertThat(friendshipRepository.findById("1_2")).isPresent();

        // when
        friendshipRepository.deleteById("1_2");

        // then
        assertThat(friendshipRepository.findById("1_2")).isEmpty();
    }

    @Test
    @DisplayName("친구 관계 삭제 후 existsFriendshipBetween은 false를 반환한다")
    void deleteById_ExistenceCheck() {
        // given
        friendshipRepository.save(FriendTestFactory.createFriendship(userA, userB));
        assertThat(friendshipRepository.existsFriendshipBetween(1L, 2L)).isTrue();

        // when
        friendshipRepository.deleteById("1_2");

        // then
        assertThat(friendshipRepository.existsFriendshipBetween(1L, 2L)).isFalse();
    }

    // --- [새롭게 추가된 핵심 테스트들] ---

    @Test
    @DisplayName("특정 사용자의 모든 Friendship 엔티티를 통째로 조회한다")
    void findFriendshipsBy_User_Id_Success() {
        // given
        friendshipRepository.save(FriendTestFactory.createFriendship(userA, userB));
        friendshipRepository.save(FriendTestFactory.createFriendship(userA, userC));

        // when
        List<Friendship> friendships = friendshipRepository.findFriendshipsByUserId(userA.getId());

        // then
        assertThat(friendships).hasSize(2);
    }

    @Test
    @DisplayName("특정 타겟 ID 목록에 해당하는 Friendship 엔티티만 필터링하여 조회한다")
    void findFriendshipsIn_Success() {
        // given
        friendshipRepository.save(FriendTestFactory.createFriendship(userA, userB));
        friendshipRepository.save(FriendTestFactory.createFriendship(userA, userC));

        // when (userB만 타겟으로 지정)
        List<Friendship> friendships = friendshipRepository.findFriendshipsIn(userA.getId(), Set.of(userB.getId()));

        // then
        assertThat(friendships).hasSize(1);
        assertThat(friendships.get(0).getFriend(userA.getId()).getId()).isEqualTo(userB.getId());
    }

    @Test
    @DisplayName("음소거 상태를 필터링하여 Friendship 엔티티를 조회한다")
    void findFriendshipsByMuteStatus_Success() {
        // given
        Friendship friendshipWithB = FriendTestFactory.createFriendship(userA, userB);
        friendshipWithB.updateMuteStatus(userA.getId(), true); // B는 음소거
        friendshipRepository.save(friendshipWithB);

        Friendship friendshipWithC = FriendTestFactory.createFriendship(userA, userC);
        friendshipWithC.updateMuteStatus(userA.getId(), false); // C는 활성
        friendshipRepository.save(friendshipWithC);

        // when
        List<Friendship> mutedFriendships = friendshipRepository.findFriendshipsByMuteStatus(userA.getId(), true);
        List<Friendship> activeFriendships = friendshipRepository.findFriendshipsByMuteStatus(userA.getId(), false);

        // then
        assertThat(mutedFriendships).hasSize(1);
        assertThat(mutedFriendships.get(0).getFriend(userA.getId()).getId()).isEqualTo(userB.getId());

        assertThat(activeFriendships).hasSize(1);
        assertThat(activeFriendships.get(0).getFriend(userA.getId()).getId()).isEqualTo(userC.getId());
    }
}