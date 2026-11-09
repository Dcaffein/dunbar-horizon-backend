package com.example.DunbarHorizon.social.adapter.out;

import com.example.DunbarHorizon.social.adapter.out.persistence.neo4j.FriendshipRepositoryAdapter;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Import;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@Neo4jRepositoryTest
@Import(FriendshipRepositoryAdapter.class)
class FriendshipNeo4jRepositoryTest {

    @Autowired
    private FriendshipRepositoryAdapter friendshipRepository;

    @Autowired
    private SocialUserNeo4jRepository socialUserNeo4jRepository;

    @Autowired
    private Neo4jClient neo4jClient;

    @Autowired
    @Qualifier("neo4jTransactionManager")
    private PlatformTransactionManager transactionManager;

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
        friendshipRepository.create(friendship);

        // when
        Optional<Friendship> found = friendshipRepository.findById("1_2");

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getIntimacy()).isGreaterThanOrEqualTo(0.0);
    }

    @Test
    @DisplayName("사용자별 필드를 수정해도 FRIENDS_WITH 관계가 하나로 유지되고 상대 쪽 값은 바뀌지 않는다")
    void updateUserFields_lowSide_preservesSingleRelationship() {
        // given
        friendshipRepository.create(FriendTestFactory.createFriendship(userA, userB));
        Friendship existing = friendshipRepository.findById("1_2").orElseThrow();
        existing.updateUserFields(userA.getId(), "수정 별명", true, false);

        // when
        friendshipRepository.updateUserFields(existing, userA.getId());

        // then
        Friendship reloaded = friendshipRepository.findById("1_2").orElseThrow();
        Long relationshipCount = neo4jClient.query("MATCH ()-[r:FRIENDS_WITH]->() RETURN count(r)")
                .fetchAs(Long.class)
                .one()
                .orElseThrow();

        assertThat(relationshipCount).isEqualTo(1L);
        assertThat(reloaded.getFriendAlias(userA.getId())).isEqualTo("수정 별명");
        assertThat(reloaded.isMuted(userA.getId())).isTrue();
        assertThat(reloaded.isRoutable(userA.getId())).isFalse();
        assertThat(reloaded.getFriendAlias(userB.getId())).isEmpty();
        assertThat(reloaded.isMuted(userB.getId())).isFalse();
        assertThat(reloaded.isRoutable(userB.getId())).isTrue();
    }

    @Test
    @DisplayName("높은 id 사용자의 필드 수정은 그 사용자 쪽 값만 바꾼다")
    void updateUserFields_highSide_updatesOnlyHighSide() {
        // given
        friendshipRepository.create(FriendTestFactory.createFriendship(userA, userB));
        Friendship existing = friendshipRepository.findById("1_2").orElseThrow();
        existing.updateUserFields(userB.getId(), "B의 별명", true, false);

        // when
        friendshipRepository.updateUserFields(existing, userB.getId());

        // then
        Friendship reloaded = friendshipRepository.findById("1_2").orElseThrow();
        assertThat(reloaded.getFriendAlias(userB.getId())).isEqualTo("B의 별명");
        assertThat(reloaded.isMuted(userB.getId())).isTrue();
        assertThat(reloaded.isRoutable(userB.getId())).isFalse();
        assertThat(reloaded.getFriendAlias(userA.getId())).isEmpty();
        assertThat(reloaded.isMuted(userA.getId())).isFalse();
        assertThat(reloaded.isRoutable(userA.getId())).isTrue();
    }

    @Test
    @DisplayName("existsFriendshipBetween은 방향에 관계없이 친구 여부를 확인한다")
    void existsFriendshipBetween_Success() {
        // given
        friendshipRepository.create(FriendTestFactory.createFriendship(userA, userB));

        // when & then
        assertThat(friendshipRepository.existsFriendshipBetween(1L, 2L)).isTrue();
        assertThat(friendshipRepository.existsFriendshipBetween(2L, 1L)).isTrue();
        assertThat(friendshipRepository.existsFriendshipBetween(1L, 3L)).isFalse();
    }

    @Test
    @DisplayName("특정 ID 목록 중 친구인 ID들만 필터링하여 반환한다")
    void filterFriendIdsAmong_Success() {
        // given
        friendshipRepository.create(FriendTestFactory.createFriendship(userA, userB));

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
        friendship.updateUserFields(userA.getId(), null, true, null);
        friendshipRepository.create(friendship);

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
        friendshipRepository.create(friendship);

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
        friendshipRepository.create(friendship);

        // userA 쪽만 60일 전으로 설정 → decayTime(30일 전) 조건 충족
        // userB 쪽은 생성 직후 → decayTime 조건 미충족 (최신)
        neo4jClient.query(
                "MATCH ()-[r:FRIENDS_WITH {id: $fid}]->() SET r.lowLastInteractedAt = $past"
        ).bind("1_2").to("fid")
         .bind(LocalDateTime.now().minusDays(60)).to("past")
         .run();

        // when
        friendshipRepository.applyDecay(0.967, 1.0, LocalDateTime.now().minusDays(30));

        // then — 버그 시 intimacy = 0.0, 수정 후 양쪽 점수 기반 정상 계산값 > 0
        Friendship updated = friendshipRepository.findById("1_2").orElseThrow();
        assertThat(updated.getIntimacy()).isGreaterThan(0.0);
    }

    @Test
    @DisplayName("단방향 관심도 갱신은 행위자 쪽 마지막 교류 시각만 바꾼다")
    void saveInterestScores_oneSided_updatesOnlyActorLastInteractedAt() {
        // given
        Friendship friendship = FriendTestFactory.createFriendship(userA, userB);
        friendshipRepository.create(friendship);
        LocalDateTime past = ageLastInteractions(friendship.getId());

        // when
        saveAfter(friendship.getId(), loaded -> loaded.adjustInterestScore(userB.getId(), 1.0));

        // then
        assertThat(lastInteractedAtOf(friendship.getId(), userA.getId())).isEqualTo(past);
        assertThat(lastInteractedAtOf(friendship.getId(), userB.getId())).isAfter(past);
    }

    @Test
    @DisplayName("상호 관심도 갱신은 양쪽 마지막 교류 시각을 모두 바꾼다")
    void saveInterestScores_mutual_updatesBothLastInteractedAt() {
        // given
        Friendship friendship = FriendTestFactory.createFriendship(userA, userB);
        friendshipRepository.create(friendship);
        LocalDateTime past = ageLastInteractions(friendship.getId());

        // when
        saveAfter(friendship.getId(), loaded -> loaded.adjustMutualInterestScore(1.0));

        // then
        assertThat(lastInteractedAtOf(friendship.getId(), userA.getId())).isAfter(past);
        assertThat(lastInteractedAtOf(friendship.getId(), userB.getId())).isAfter(past);
    }

    @Test
    @DisplayName("친구 관계를 삭제하면 더 이상 조회되지 않는다")
    void deleteById_Success() {
        // given
        Friendship friendship = FriendTestFactory.createFriendship(userA, userB);
        friendshipRepository.create(friendship);
        assertThat(friendshipRepository.findById("1_2")).isPresent();

        // when
        friendshipRepository.delete("1_2");

        // then
        assertThat(friendshipRepository.findById("1_2")).isEmpty();
    }

    @Test
    @DisplayName("친구 관계 삭제 후 existsFriendshipBetween은 false를 반환한다")
    void deleteById_ExistenceCheck() {
        // given
        friendshipRepository.create(FriendTestFactory.createFriendship(userA, userB));
        assertThat(friendshipRepository.existsFriendshipBetween(1L, 2L)).isTrue();

        // when
        friendshipRepository.delete("1_2");

        // then
        assertThat(friendshipRepository.existsFriendshipBetween(1L, 2L)).isFalse();
    }

    @Test
    @DisplayName("특정 사용자의 모든 Friendship 엔티티를 통째로 조회한다")
    void findByUserId_Success() {
        // given
        friendshipRepository.create(FriendTestFactory.createFriendship(userA, userB));
        friendshipRepository.create(FriendTestFactory.createFriendship(userA, userC));

        // when
        List<Friendship> friendships = friendshipRepository.findByUserId(userA.getId());

        // then
        assertThat(friendships).hasSize(2);
    }

    @Test
    @DisplayName("findByUserId — 비활성 친구가 있는 friendship은 결과에서 제외된다")
    void findByUserId_excludesInactiveFriend() {
        // given
        friendshipRepository.create(FriendTestFactory.createFriendship(userA, userB));
        friendshipRepository.create(FriendTestFactory.createFriendship(userA, userC));
        userC.switchUserStatus(false);
        socialUserNeo4jRepository.save(userC);

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
        friendshipRepository.create(FriendTestFactory.createFriendship(userA, userB));
        friendshipRepository.create(FriendTestFactory.createFriendship(userA, userC));
        userC.switchUserStatus(false);
        socialUserNeo4jRepository.save(userC);

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
        friendshipAB.updateUserFields(userA.getId(), null, true, null);
        friendshipRepository.create(friendshipAB);

        Friendship friendshipAC = FriendTestFactory.createFriendship(userA, userC);
        friendshipAC.updateUserFields(userA.getId(), null, true, null);
        friendshipRepository.create(friendshipAC);
        userC.switchUserStatus(false);
        socialUserNeo4jRepository.save(userC);

        // when
        Set<Long> mutedIds = friendshipRepository.findFriendIdsByMuteStatus(userA.getId(), true);

        // then — 활성 userB만 포함, 비활성 userC는 제외
        assertThat(mutedIds).containsExactly(userB.getId());
    }

    @Test
    @DisplayName("updateUserFields — 비활성 유저 본인의 관계 필드는 수정되지 않는다")
    void updateUserFields_doesNotUpdateInactiveUser() {
        // given
        Friendship friendship = FriendTestFactory.createFriendship(userA, userB);
        friendshipRepository.create(friendship);
        friendship.updateUserFields(userA.getId(), "별명", false, false);

        userA.switchUserStatus(false);
        socialUserNeo4jRepository.save(userA);

        // when — 비활성 userA의 필드 수정 시도
        friendshipRepository.updateUserFields(friendship, userA.getId());

        // then — UserReference 레이블이 없는 비활성 유저는 쿼리 MATCH에서 제외되어 SET이 무시됨
        Optional<String> alias = neo4jClient.query(
                "MATCH ()-[r:FRIENDS_WITH {id: $fid}]->() RETURN r.lowFriendAlias"
        ).bind(friendship.getId()).to("fid")
         .fetchAs(String.class)
         .one();
        assertThat(alias).isEmpty();
    }

    @Test
    @DisplayName("친구가 비활성이면 findById는 비어 있고 친구 관계 존재 여부는 유지된다")
    void findById_inactiveFriend_emptyButExists() {
        // given
        friendshipRepository.create(FriendTestFactory.createFriendship(userA, userB));
        userB.switchUserStatus(false);
        socialUserNeo4jRepository.save(userB);

        // when & then
        assertThat(friendshipRepository.findById("1_2")).isEmpty();
        assertThat(friendshipRepository.existsFriendshipBetween(1L, 2L)).isTrue();
    }

    @Test
    @DisplayName("친구가 비활성이어도 친구 관계를 삭제할 수 있다")
    void delete_inactiveFriend_Success() {
        // given
        friendshipRepository.create(FriendTestFactory.createFriendship(userA, userB));
        userB.switchUserStatus(false);
        socialUserNeo4jRepository.save(userB);

        // when
        friendshipRepository.delete("1_2");

        // then
        assertThat(friendshipRepository.existsFriendshipBetween(1L, 2L)).isFalse();
    }

    @Test
    @DisplayName("비활성화했다가 재활성화하면 사용자별 필드가 그대로 복구된다")
    void reactivation_restoresFriendship() {
        // given
        Friendship friendship = FriendTestFactory.createFriendship(userA, userB);
        friendship.updateUserFields(userA.getId(), "별명", true, false);
        friendshipRepository.create(friendship);
        userB.switchUserStatus(false);
        socialUserNeo4jRepository.save(userB);

        // when
        userB.switchUserStatus(true);
        socialUserNeo4jRepository.save(userB);

        // then
        Friendship restored = friendshipRepository.findById("1_2").orElseThrow();
        assertThat(restored.getFriendAlias(userA.getId())).isEqualTo("별명");
        assertThat(restored.isMuted(userA.getId())).isTrue();
        assertThat(restored.isRoutable(userA.getId())).isFalse();
    }

    @Test
    @DisplayName("친구가 비활성이면 감쇄가 그 친구 관계를 건드리지 않는다")
    void applyDecay_inactiveFriend_leavesFriendshipUntouched() {
        // given
        Friendship friendship = FriendTestFactory.createFriendship(userA, userB);
        friendshipRepository.create(friendship);
        userB.switchUserStatus(false);
        socialUserNeo4jRepository.save(userB);

        // when
        friendshipRepository.applyDecay(0.5, 1.0, LocalDateTime.now().plusSeconds(1));

        // then
        userB.switchUserStatus(true);
        socialUserNeo4jRepository.save(userB);
        Friendship restored = friendshipRepository.findById("1_2").orElseThrow();
        assertThat(restored.getIntimacy()).isCloseTo(friendship.getIntimacy(), within(0.0001));
        assertThat(restored.getMyInterestScore(userA.getId()))
                .isCloseTo(FriendRecognition.INITIAL_RAW_SCORE, within(0.0001));
    }

    @Test
    @DisplayName("높은 id 사용자의 단방향 관심도 갱신은 그 사용자 쪽 점수만 바꾼다")
    void saveInterestScores_highSide_updatesOnlyHighSide() {
        // given
        Friendship actual = FriendTestFactory.createFriendship(userA, userB);
        Friendship expected = FriendTestFactory.createFriendship(userA, userB);
        friendshipRepository.create(actual);
        expected.adjustInterestScore(userB.getId(), 10.0);

        // when
        saveAfter(actual.getId(), loaded -> loaded.adjustInterestScore(userB.getId(), 10.0));

        // then
        Friendship updated = friendshipRepository.findById(actual.getId()).orElseThrow();
        assertThat(updated.getMyInterestScore(userB.getId()))
                .isCloseTo(expected.getMyInterestScore(userB.getId()), within(0.0001));
        assertThat(updated.getMyInterestScore(userA.getId()))
                .isCloseTo(FriendRecognition.INITIAL_RAW_SCORE, within(0.0001));
        assertThat(updated.getIntimacy()).isCloseTo(expected.getIntimacy(), within(0.0001));
    }

    @Test
    @DisplayName("여러 관계를 한 번에 불러오고, 없는 관계와 비활성 친구의 관계는 건너뛴다")
    void findAllByIdsForUpdate_skipsMissingAndInactive() {
        // given
        SocialUser userD = socialUserNeo4jRepository.save(new SocialUser(4L, "사용자D", "url4"));
        friendshipRepository.create(FriendTestFactory.createFriendship(userA, userB));
        friendshipRepository.create(FriendTestFactory.createFriendship(userA, userC));
        friendshipRepository.create(FriendTestFactory.createFriendship(userA, userD));
        userD.switchUserStatus(false);
        socialUserNeo4jRepository.save(userD);

        // when
        List<Friendship> found = friendshipRepository.findAllByIdsForUpdate(List.of("1_2", "1_3", "1_4", "2_3"));

        // then
        assertThat(found).extracting(Friendship::getId).containsExactlyInAnyOrder("1_2", "1_3");
    }

    @Test
    @DisplayName("점수 저장은 관심도, 교류 시각, 친밀도만 쓰고 사용자별 설정은 건드리지 않는다")
    void saveInterestScores_keepsUserFields() {
        // given
        Friendship friendship = FriendTestFactory.createFriendship(userA, userB);
        friendship.updateUserFields(userA.getId(), "A의 별명", true, false);
        friendshipRepository.create(friendship);

        // when
        saveAfter(friendship.getId(), loaded -> loaded.adjustMutualInterestScore(10.0));

        // then
        Friendship updated = friendshipRepository.findById(friendship.getId()).orElseThrow();
        assertThat(updated.getFriendAlias(userA.getId())).isEqualTo("A의 별명");
        assertThat(updated.isMuted(userA.getId())).isTrue();
        assertThat(updated.isRoutable(userA.getId())).isFalse();
        assertThat(updated.getMyInterestScore(userA.getId()))
                .isCloseTo(FriendRecognition.INITIAL_RAW_SCORE + 10.0, within(0.0001));
        assertThat(updated.getIntimacy())
                .isCloseTo(FriendRecognition.normalize(FriendRecognition.INITIAL_RAW_SCORE + 10.0), within(0.0001));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("수정용 로드는 다른 트랜잭션의 잠금이 풀릴 때까지 기다린 뒤 그 트랜잭션이 쓴 값을 읽는다")
    void findAllByIdsForUpdate_ReadsCommittedValue_AfterWaitingForLock() throws Exception {
        // given
        Friendship friendship = FriendTestFactory.createFriendship(userA, userB);
        friendshipRepository.create(friendship);
        double committedScore = 99.0;
        long holdMillis = 500L;
        CountDownLatch locked = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            Future<?> holder = executor.submit(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                neo4jClient.query("MATCH ()-[r:FRIENDS_WITH {id: $id}]->() SET r.lowInterestScore = $score")
                        .bind(friendship.getId()).to("id")
                        .bind(committedScore).to("score")
                        .run();
                locked.countDown();
                sleep(holdMillis);
            }));
            locked.await();

            // when
            long startedAt = System.nanoTime();
            Friendship loaded = new TransactionTemplate(transactionManager).execute(status ->
                    friendshipRepository.findAllByIdsForUpdate(List.of(friendship.getId())).get(0));
            long waitedMillis = (System.nanoTime() - startedAt) / 1_000_000;
            holder.get();

            // then
            assertThat(waitedMillis).isGreaterThanOrEqualTo(holdMillis / 2);
            assertThat(loaded.getMyInterestScore(userA.getId())).isEqualTo(committedScore);
        } finally {
            executor.shutdownNow();
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    private void saveAfter(String friendshipId, Consumer<Friendship> adjustment) {
        List<Friendship> friendships = friendshipRepository.findAllByIdsForUpdate(List.of(friendshipId));
        friendships.forEach(adjustment);
        friendshipRepository.saveInterestScores(friendships);
    }

    private LocalDateTime ageLastInteractions(String friendshipId) {
        LocalDateTime past = LocalDateTime.now().minusDays(60);
        neo4jClient.query(
                "MATCH ()-[r:FRIENDS_WITH {id: $fid}]->() " +
                "SET r.lowLastInteractedAt = $past, r.highLastInteractedAt = $past"
        ).bind(friendshipId).to("fid")
         .bind(past).to("past")
         .run();
        return past;
    }

    private LocalDateTime lastInteractedAtOf(String friendshipId, Long userId) {
        return friendshipRepository.findById(friendshipId).orElseThrow().getRecognitions().stream()
                .filter(recognition -> recognition.getUser().getId().equals(userId))
                .findFirst()
                .orElseThrow()
                .getLastInteractedAt();
    }
}
