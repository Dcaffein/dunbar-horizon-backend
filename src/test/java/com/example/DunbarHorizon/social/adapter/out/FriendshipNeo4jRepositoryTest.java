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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
    @DisplayName("기존 Friendship을 수정 후 다시 저장해도 두 HAS_FRIENDSHIP 관계를 유지한다")
    void saveExistingFriendshipAfterRelationshipUpdate_preservesRelationships() {
        // given
        friendshipRepository.save(FriendTestFactory.createFriendship(userA, userB));
        Friendship existing = friendshipRepository.findById("1_2").orElseThrow();
        existing.updateUserFields(userA.getId(), "수정 별명", true, false);

        // when
        friendshipRepository.save(existing);

        // then
        Friendship reloaded = friendshipRepository.findById("1_2").orElseThrow();
        Long relationshipCount = neo4jClient.query(
                "MATCH (:UserReference)-[:HAS_FRIENDSHIP]->(f:Friendship {id: $friendshipId}) RETURN count(*)"
        ).bind("1_2").to("friendshipId")
                .fetchAs(Long.class)
                .one()
                .orElseThrow();

        assertThat(relationshipCount).isEqualTo(2L);
        assertThat(reloaded.getFriendAlias(userA.getId())).isEqualTo("수정 별명");
        assertThat(reloaded.isMuted(userA.getId())).isTrue();
        assertThat(reloaded.isRoutable(userA.getId())).isFalse();
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
    void filterFriendIdsAmong_Success() {
        // given
        friendshipRepository.save(FriendTestFactory.createFriendship(userA, userB));

        // when
        Set<Long> friendIds = friendshipRepository.filterFriendIdsAmong(1L, Set.of(2L, 3L));

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
    @DisplayName("단방향 관심도 Cypher 갱신은 aggregate 정책과 동일한 score와 intimacy를 남긴다")
    void incrementInterestScore_matchesAggregatePolicy() {
        // given
        Friendship actual = FriendTestFactory.createFriendship(userA, userB);
        Friendship expected = FriendTestFactory.createFriendship(userA, userB);
        friendshipRepository.save(actual);
        expected.adjustInterestScore(userA.getId(), 10.0);

        // when
        friendshipRepository.incrementInterestScore(actual.getId(), userA.getId(), userB.getId(), 10.0);

        // then
        Friendship updated = friendshipRepository.findById(actual.getId()).orElseThrow();
        assertThat(updated.getMyInterestScore(userA.getId()))
                .isCloseTo(expected.getMyInterestScore(userA.getId()), within(0.0001));
        assertThat(updated.getMyInterestScore(userB.getId()))
                .isCloseTo(expected.getMyInterestScore(userB.getId()), within(0.0001));
        assertThat(updated.getIntimacy()).isCloseTo(expected.getIntimacy(), within(0.0001));
    }

    @Test
    @DisplayName("상호 관심도 Cypher 갱신은 aggregate 정책과 동일하게 양쪽 score와 intimacy를 갱신한다")
    void incrementMutualInterestScore_matchesAggregatePolicy() {
        // given
        Friendship actual = FriendTestFactory.createFriendship(userA, userB);
        Friendship expected = FriendTestFactory.createFriendship(userA, userB);
        friendshipRepository.save(actual);
        expected.adjustMutualInterestScore(10.0);

        // when
        friendshipRepository.incrementMutualInterestScore(actual.getId(), userA.getId(), userB.getId(), 10.0);

        // then
        Friendship updated = friendshipRepository.findById(actual.getId()).orElseThrow();
        assertThat(updated.getMyInterestScore(userA.getId()))
                .isCloseTo(expected.getMyInterestScore(userA.getId()), within(0.0001));
        assertThat(updated.getMyInterestScore(userB.getId()))
                .isCloseTo(expected.getMyInterestScore(userB.getId()), within(0.0001));
        assertThat(updated.getIntimacy()).isCloseTo(expected.getIntimacy(), within(0.0001));
    }

    @Test
    @DisplayName("상호 관심도 batch Cypher는 모든 Friendship의 score와 intimacy를 갱신한다")
    void incrementMutualInterestScoresBatch_updatesEveryPair() {
        // given
        Friendship friendshipAB = FriendTestFactory.createFriendship(userA, userB);
        Friendship friendshipAC = FriendTestFactory.createFriendship(userA, userC);
        friendshipRepository.save(friendshipAB);
        friendshipRepository.save(friendshipAC);

        // when
        friendshipRepository.incrementMutualInterestScoresBatch(List.of(
                Map.of("friendshipId", friendshipAC.getId(), "userAId", userA.getId(), "userBId", userC.getId(), "delta", 10.0),
                Map.of("friendshipId", friendshipAB.getId(), "userAId", userA.getId(), "userBId", userB.getId(), "delta", 20.0)
        ));

        // then
        Friendship updatedAB = friendshipRepository.findById(friendshipAB.getId()).orElseThrow();
        Friendship updatedAC = friendshipRepository.findById(friendshipAC.getId()).orElseThrow();
        assertThat(updatedAB.getMyInterestScore(userA.getId()))
                .isCloseTo(FriendRecognition.INITIAL_RAW_SCORE + 20.0, within(0.0001));
        assertThat(updatedAB.getMyInterestScore(userB.getId()))
                .isCloseTo(FriendRecognition.INITIAL_RAW_SCORE + 20.0, within(0.0001));
        assertThat(updatedAC.getMyInterestScore(userA.getId()))
                .isCloseTo(FriendRecognition.INITIAL_RAW_SCORE + 10.0, within(0.0001));
        assertThat(updatedAC.getMyInterestScore(userC.getId()))
                .isCloseTo(FriendRecognition.INITIAL_RAW_SCORE + 10.0, within(0.0001));
        assertThat(updatedAB.getIntimacy()).isCloseTo(
                FriendRecognition.normalize(FriendRecognition.INITIAL_RAW_SCORE + 20.0), within(0.0001)
        );
        assertThat(updatedAC.getIntimacy()).isCloseTo(
                FriendRecognition.normalize(FriendRecognition.INITIAL_RAW_SCORE + 10.0), within(0.0001)
        );
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("동일 Friendship의 병렬 갱신은 이벤트 수만큼 누적되고 최종 intimacy를 재계산한다")
    void incrementInterestScore_concurrently_accumulatesEveryEvent() throws Exception {
        // given
        Friendship friendship = friendshipRepository.save(FriendTestFactory.createFriendship(userA, userB));
        int eventCount = 16;
        ExecutorService executor = Executors.newFixedThreadPool(eventCount);
        CountDownLatch ready = new CountDownLatch(eventCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < eventCount; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    friendshipRepository.incrementInterestScore(
                            friendship.getId(), userA.getId(), userB.getId(), 1.0
                    );
                    return null;
                }));
            }
            ready.await();

            // when
            start.countDown();
            for (Future<?> future : futures) {
                future.get();
            }
        } finally {
            executor.shutdownNow();
        }

        // then
        Friendship updated = friendshipRepository.findById(friendship.getId()).orElseThrow();
        double expectedScoreA = FriendRecognition.INITIAL_RAW_SCORE + eventCount;
        double expectedScoreB = FriendRecognition.INITIAL_RAW_SCORE;
        double expectedIntimacy = Math.sqrt(
                FriendRecognition.normalize(expectedScoreA) * FriendRecognition.normalize(expectedScoreB)
        );
        assertThat(updated.getMyInterestScore(userA.getId())).isCloseTo(expectedScoreA, within(0.0001));
        assertThat(updated.getMyInterestScore(userB.getId())).isCloseTo(expectedScoreB, within(0.0001));
        assertThat(updated.getIntimacy()).isCloseTo(expectedIntimacy, within(0.0001));
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

    @Test
    @DisplayName("특정 사용자의 모든 Friendship 엔티티를 통째로 조회한다")
    void findByUserId_Success() {
        // given
        friendshipRepository.save(FriendTestFactory.createFriendship(userA, userB));
        friendshipRepository.save(FriendTestFactory.createFriendship(userA, userC));

        // when
        List<Friendship> friendships = friendshipRepository.findByUserId(userA.getId());

        // then
        assertThat(friendships).hasSize(2);
    }

    @Test
    @DisplayName("findByUserId — 비활성 친구가 있는 friendship은 결과에서 제외된다")
    void findByUserId_excludesInactiveFriend() {
        // given
        friendshipRepository.save(FriendTestFactory.createFriendship(userA, userB));
        userC.switchUserStatus(false);
        socialUserNeo4jRepository.save(userC);
        friendshipRepository.save(FriendTestFactory.createFriendship(userA, userC));

        // when
        List<Friendship> friendships = friendshipRepository.findByUserId(userA.getId());

        // then — 비활성 userC와의 friendship은 제외, 활성 userB와의 friendship만 반환
        assertThat(friendships).hasSize(1);
        assertThat(friendships.get(0).getFriend(userA.getId()).getId()).isEqualTo(userB.getId());
    }

    @Test
    @DisplayName("filterFriendIdsAmong — 비활성 친구는 친구로 인식되지 않는다")
    void filterFriendIdsAmong_excludesInactiveFriend() {
        // given
        friendshipRepository.save(FriendTestFactory.createFriendship(userA, userB));
        userC.switchUserStatus(false);
        socialUserNeo4jRepository.save(userC);
        friendshipRepository.save(FriendTestFactory.createFriendship(userA, userC));

        // when
        Set<Long> friendIds = friendshipRepository.filterFriendIdsAmong(userA.getId(), Set.of(userB.getId(), userC.getId()));

        // then — 활성 userB만 포함, 비활성 userC는 제외
        assertThat(friendIds).containsExactly(userB.getId());
    }

    @Test
    @DisplayName("findFriendIdsByMuteStatus — 비활성 친구는 묵음 목록에 포함되지 않는다")
    void findFriendIdsByMuteStatus_excludesInactiveFriend() {
        // given — userB(활성) 묵음, userC(비활성) 묵음
        Friendship friendshipAB = FriendTestFactory.createFriendship(userA, userB);
        friendshipAB.updateMuteStatus(userA.getId(), true);
        friendshipRepository.save(friendshipAB);

        userC.switchUserStatus(false);
        socialUserNeo4jRepository.save(userC);
        Friendship friendshipAC = FriendTestFactory.createFriendship(userA, userC);
        friendshipAC.updateMuteStatus(userA.getId(), true);
        friendshipRepository.save(friendshipAC);

        // when
        Set<Long> mutedIds = friendshipRepository.findFriendIdsByMuteStatus(userA.getId(), true);

        // then — 활성 userB만 포함, 비활성 userC는 제외
        assertThat(mutedIds).containsExactly(userB.getId());
    }

    @Test
    @DisplayName("updateUserRelationshipFields — 비활성 유저 본인의 관계 필드는 수정되지 않는다")
    void updateUserRelationshipFields_doesNotUpdateInactiveUser() {
        // given
        Friendship friendship = FriendTestFactory.createFriendship(userA, userB);
        friendshipRepository.save(friendship);

        userA.switchUserStatus(false);
        socialUserNeo4jRepository.save(userA);

        // when — 비활성 userA의 필드 수정 시도
        friendshipRepository.updateUserRelationshipFields(friendship.getId(), userA.getId(), "별명", false, false);

        // then — UserReference 레이블이 없는 비활성 유저는 쿼리 MATCH에서 제외되어 SET이 무시됨
        // findById도 동일한 레이블 제약으로 실패하므로 Neo4jClient로 직접 검증
        Optional<String> alias = neo4jClient.query(
                "MATCH (:SocialUser {id: $userId})-[r:HAS_FRIENDSHIP]->(f:Friendship {id: $fid}) RETURN r.friendAlias"
        ).bind(userA.getId()).to("userId")
         .bind(friendship.getId()).to("fid")
         .fetchAs(String.class)
         .one();
        assertThat(alias).isEmpty();
    }
}
