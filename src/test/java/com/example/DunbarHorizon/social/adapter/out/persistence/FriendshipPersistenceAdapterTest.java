package com.example.DunbarHorizon.social.adapter.out.persistence;

import com.example.DunbarHorizon.account.domain.User;
import com.example.DunbarHorizon.social.adapter.out.persistence.jdbc.FriendshipJdbcRepository;
import com.example.DunbarHorizon.social.domain.friend.FriendRecognition;
import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.social.domain.friend.exception.AlreadyFriendsException;
import com.example.DunbarHorizon.support.JpaRepositoryTest;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@JpaRepositoryTest
@Import({FriendshipPersistenceAdapter.class, FriendshipJdbcRepository.class})
class FriendshipPersistenceAdapterTest {

    private static final LocalDate CREATED_AT = LocalDate.of(2026, 9, 14);
    private static final LocalDateTime INTERACTED_AT = CREATED_AT.atTime(12, 30);
    private static final double INTIMACY =
            Math.sqrt(FriendRecognition.normalize(51.0) * FriendRecognition.normalize(32.0));

    @Autowired private FriendshipPersistenceAdapter repository;
    @Autowired private TestEntityManager em;
    @Autowired private JdbcTemplate jdbc;

    private User low;
    private User high;

    @BeforeEach
    void setUp() {
        low = createUser("low");
        high = createUser("high");
    }

    @Test
    @DisplayName("두 사용자 방향에서 같은 관계 한 건을 읽고 각자의 설정을 복원한다")
    void findBetween_ReturnsSameRelationship_FromBothDirections() {
        // given
        repository.create(friendship(low, high));
        em.clear();

        // when
        var fromLow = repository.findBetween(low.getId(), high.getId());
        var fromHigh = repository.findBetween(high.getId(), low.getId());

        // then
        assertThat(fromLow).get().satisfies(friendship -> {
            assertThat(friendship.getUserIds()).containsExactlyInAnyOrder(low.getId(), high.getId());
            assertThat(friendship.getFriendId(low.getId())).isEqualTo(high.getId());
            assertThat(friendship.getCreatedAt()).isEqualTo(CREATED_AT);
            assertThat(friendship.getIntimacy()).isEqualTo(INTIMACY);
            assertThat(friendship.getFriendAlias(low.getId())).isEqualTo("from low");
            assertThat(friendship.getMyInterestScore(low.getId())).isEqualTo(51.0);
            assertThat(friendship.isMuted(low.getId())).isTrue();
            assertThat(friendship.isRoutable(low.getId())).isFalse();
        });
        assertThat(fromHigh).get().satisfies(friendship -> {
            assertThat(friendship.getUserIds()).containsExactlyInAnyOrder(low.getId(), high.getId());
            assertThat(friendship.getFriendId(high.getId())).isEqualTo(low.getId());
            assertThat(friendship.getFriendAlias(high.getId())).isEqualTo("from high");
            assertThat(friendship.getMyInterestScore(high.getId())).isEqualTo(32.0);
            assertThat(friendship.isMuted(high.getId())).isFalse();
            assertThat(friendship.isRoutable(high.getId())).isTrue();
        });
        assertThat(repository.existsBetween(high.getId(), low.getId())).isTrue();
    }

    @Test
    @DisplayName("활성 사용자 쌍의 단건 조회, 존재 확인, 삭제는 각각 SQL 한 문장으로 처리한다")
    void pairOperations_UseOneStatementEach_ForActiveUsers() {
        // given
        repository.create(friendship(low, high));
        em.clear();
        var statistics = em.getEntityManager().getEntityManagerFactory()
                .unwrap(SessionFactory.class).getStatistics();
        boolean wasEnabled = statistics.isStatisticsEnabled();
        statistics.setStatisticsEnabled(true);

        try {
            // when
            long beforeFind = statistics.getPrepareStatementCount();
            var found = repository.findBetween(high.getId(), low.getId());
            long afterFind = statistics.getPrepareStatementCount();
            boolean exists = repository.existsBetween(high.getId(), low.getId());
            long afterExists = statistics.getPrepareStatementCount();
            boolean deleted = repository.deleteBetween(high.getId(), low.getId());
            long afterDelete = statistics.getPrepareStatementCount();

            // then
            assertThat(found).isPresent();
            assertThat(exists).isTrue();
            assertThat(deleted).isTrue();
            assertThat(List.of(afterFind - beforeFind, afterExists - afterFind, afterDelete - afterExists))
                    .as("단건 조회, 존재 확인, 삭제의 SQL 문장 수")
                    .containsExactly(1L, 1L, 1L);
        } finally {
            statistics.setStatisticsEnabled(wasEnabled);
        }
    }

    @Test
    @DisplayName("이미 친구인 쌍을 다시 생성하면 친구 중복 예외로 끝나고 기존 행의 설정과 점수는 그대로다")
    void create_ThrowsAlreadyFriends_DuplicatePair() {
        // given
        repository.create(friendship(low, high));
        em.clear();
        Friendship duplicate = friendship(high, low);
        duplicate.updateSettings(low.getId(), "overwritten", false, true);
        duplicate.adjustMutualInterestScore(10);

        // when & then
        assertThatThrownBy(() -> repository.create(duplicate)).isInstanceOf(AlreadyFriendsException.class);
        em.clear();
        assertThat(repository.findBetween(low.getId(), high.getId())).get().satisfies(friendship -> {
            assertThat(friendship.getFriendAlias(low.getId())).isEqualTo("from low");
            assertThat(friendship.isMuted(low.getId())).isTrue();
            assertThat(friendship.getMyInterestScore(low.getId())).isEqualTo(51.0);
            assertThat(friendship.getMyInterestScore(high.getId())).isEqualTo(32.0);
        });
    }

    @Test
    @DisplayName("이미 영속 상태인 쌍을 다시 생성해도 친구 중복 예외로 끝난다")
    void create_ThrowsAlreadyFriends_PairInPersistenceContext() {
        // given
        repository.create(friendship(low, high));

        // when & then
        assertThatThrownBy(() -> repository.create(friendship(high, low))).isInstanceOf(AlreadyFriendsException.class);
    }

    @Test
    @DisplayName("DB는 사용자 ID 순서가 뒤집힌 관계를 저장하지 않는다")
    void insert_RejectsReversedPair_CheckConstraint() {
        // when & then
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO friendships (
                    low_user_id, high_user_id,
                    low_interest_score, low_is_muted, low_is_routable,
                    high_interest_score, high_is_muted, high_is_routable
                ) VALUES (?, ?, 51, TRUE, FALSE, 32, FALSE, TRUE)
                """, high.getId(), low.getId()))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("chk_friendships_user_order");
    }

    @Test
    @DisplayName("설정 변경은 본인 쪽 설정 열만 쓰고 로드 뒤 바뀐 상대 설정과 양쪽 점수를 보존한다")
    void updateSettings_WritesOnlyOwnSettings_OtherColumnsChangedAfterLoad() {
        // given
        repository.create(friendship(low, high));
        em.clear();
        Friendship loaded = repository.findBetween(low.getId(), high.getId()).orElseThrow();
        jdbc.update("""
                UPDATE friendships SET low_friend_alias = 'changed by low', low_is_muted = FALSE,
                    low_interest_score = 60, high_interest_score = 40,
                    high_last_interacted_at = ?
                WHERE low_user_id = ? AND high_user_id = ?
                """, INTERACTED_AT.plusDays(2), low.getId(), high.getId());
        loaded.updateSettings(high.getId(), "", true, false);
        loaded.adjustInterestScore(high.getId(), 100);

        // when
        em.flush();
        em.clear();

        // then
        Friendship updated = repository.findBetween(low.getId(), high.getId()).orElseThrow();
        assertThat(updated.getFriendAlias(high.getId())).isEmpty();
        assertThat(updated.isMuted(high.getId())).isTrue();
        assertThat(updated.isRoutable(high.getId())).isFalse();
        assertThat(updated.getFriendAlias(low.getId())).isEqualTo("changed by low");
        assertThat(updated.isMuted(low.getId())).isFalse();
        assertThat(updated.isRoutable(low.getId())).isFalse();
        assertThat(List.of(updated.getLower(), updated.getHigher())).extracting(FriendRecognition::getInterestScore)
                .containsExactly(60.0, 40.0);
        assertThat(List.of(updated.getLower(), updated.getHigher())).extracting(FriendRecognition::getLastInteractedAt)
                .containsExactly(INTERACTED_AT, INTERACTED_AT.plusDays(2));
        assertThat(jdbc.queryForObject("SELECT high_friend_alias FROM friendships WHERE low_user_id = ?",
                String.class, low.getId())).isNull();
    }

    @Test
    @DisplayName("별명이 없는 관계는 별명 열을 NULL로 저장한다")
    void create_StoresNullAlias_EmptyAlias() {
        // given
        Friendship friendship = Friendship.restore(CREATED_AT,
                FriendRecognition.restore(low.getId(), null, 21.4, INTERACTED_AT, false, true),
                FriendRecognition.restore(high.getId(), "", 21.4, INTERACTED_AT, false, true));

        // when
        repository.create(friendship);

        // then
        assertThat(jdbc.queryForList("""
                SELECT low_friend_alias FROM friendships WHERE low_user_id = ?
                UNION ALL
                SELECT high_friend_alias FROM friendships WHERE low_user_id = ?
                """, String.class, low.getId(), low.getId())).containsExactly(null, null);
    }

    @Test
    @DisplayName("계정이 비활성화되면 모든 조회·잠금·삭제에서 관계가 없는 것으로 다뤄지고 행은 남는다")
    void lookups_TreatAsMissing_InactiveAccount() {
        // given
        repository.create(friendship(low, high));
        high.deactivate();
        em.flush();
        em.clear();
        Long me = low.getId();
        Long friend = high.getId();

        // when & then
        assertThat(repository.findBetween(me, friend)).isEmpty();
        assertThat(repository.existsBetween(me, friend)).isFalse();
        assertThat(repository.findBetweenForUpdate(me, friend)).isEmpty();
        var candidates = repository.findCandidatesWithSharedLock(me, List.of(friend));
        assertThat(candidates.activeCandidateIds()).isEmpty();
        assertThat(candidates.lockedFriendIds()).isEmpty();
        assertThat(repository.findAllAmongForUpdate(List.of(me, friend))).isEmpty();
        assertThat(repository.deleteBetween(me, friend)).isFalse();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM friendships WHERE low_user_id = ? AND high_user_id = ?",
                Integer.class, me, friend)).isEqualTo(1);
    }

    @Test
    @DisplayName("같은 사용자 두 명으로 조회하면 예외 없이 관계 없음으로 끝난다")
    void lookups_ReturnNothing_SameUser() {
        // given
        repository.create(friendship(low, high));
        Long self = low.getId();

        // when & then
        assertThat(repository.findBetween(self, self)).isEmpty();
        assertThat(repository.existsBetween(self, self)).isFalse();
        assertThat(repository.findBetweenForUpdate(self, self)).isEmpty();
        assertThat(repository.findCandidatesWithSharedLock(self, List.of(self)).lockedFriendIds()).isEmpty();
        assertThat(repository.findAllAmongForUpdate(List.of(self, self))).isEmpty();
        assertThat(repository.deleteBetween(self, self)).isFalse();
    }

    @Test
    @DisplayName("관계를 삭제하면 어느 방향에서도 조회되지 않는다")
    void deleteBetween_RemovesPair_FromBothDirections() {
        // given
        repository.create(friendship(low, high));
        em.clear();

        // when
        boolean deleted = repository.deleteBetween(high.getId(), low.getId());

        // then
        assertThat(deleted).isTrue();
        assertThat(repository.findBetween(low.getId(), high.getId())).isEmpty();
        assertThat(repository.existsBetween(high.getId(), low.getId())).isFalse();
        assertThat(repository.deleteBetween(low.getId(), high.getId())).isFalse();
    }

    @Test
    @DisplayName("한 사람과 여러 사람 사이의 잠금 조회는 실제 친구 관계만 돌려준다")
    void findCandidatesWithSharedLock_ReturnsExistingFriendships_MixedCandidates() {
        // given
        User third = createUser("third");
        repository.create(friendship(low, high));

        // when
        var candidates = repository.findCandidatesWithSharedLock(
                high.getId(), List.of(low.getId(), third.getId(), high.getId()));

        // then
        assertThat(candidates.activeCandidateIds()).containsExactlyInAnyOrder(low.getId(), third.getId(), high.getId());
        assertThat(candidates.lockedFriendIds()).containsExactly(low.getId());
    }

    @Test
    @DisplayName("공유 잠금 조회는 활성 후보와 실제로 잠근 친구를 구분한다")
    void findCandidatesWithSharedLock_FiltersCandidates_BothDirections() {
        // given
        User third = createUser("third");
        User inactive = createUser("inactive");
        repository.create(friendship(low, high));
        repository.create(friendship(high, third));
        repository.create(friendship(high, inactive));
        inactive.deactivate();
        em.flush();
        em.clear();

        // when
        var candidates = repository.findCandidatesWithSharedLock(high.getId(),
                List.of(third.getId(), low.getId(), inactive.getId(), high.getId(), third.getId(), Long.MAX_VALUE));

        // then
        assertThat(candidates.activeCandidateIds()).containsExactlyInAnyOrder(low.getId(), third.getId(), high.getId());
        assertThat(candidates.lockedFriendIds()).containsExactlyInAnyOrder(low.getId(), third.getId());
    }

    @Test
    @DisplayName("한 무리 안의 잠금 조회는 존재하는 모든 쌍을 PK 순서로 돌려준다")
    void findAllAmongForUpdate_ReturnsExistingPairsInKeyOrder_Group() {
        // given
        User third = createUser("third");
        User outsider = createUser("outsider");
        repository.create(friendship(high, third));
        repository.create(friendship(low, high));
        repository.create(friendship(low, outsider));

        // when
        var locked = repository.findAllAmongForUpdate(List.of(third.getId(), low.getId(), high.getId(), low.getId()));

        // then
        assertThat(locked).extracting(friendship -> friendship.getLower().getOwnerId() + "_"
                        + friendship.getHigher().getOwnerId())
                .containsExactly(low.getId() + "_" + high.getId(), high.getId() + "_" + third.getId());
    }

    private User createUser(String nickname) {
        return em.persistAndFlush(User.builder()
                .email(nickname + "@jpa-friends.test")
                .nickname(nickname)
                .profileImage("profiles/" + nickname)
                .build());
    }

    private static Friendship friendship(User low, User high) {
        return Friendship.restore(CREATED_AT,
                FriendRecognition.restore(high.getId(), "from high", 32.0,
                        INTERACTED_AT.plusDays(1), false, true),
                FriendRecognition.restore(low.getId(), "from low", 51.0,
                        INTERACTED_AT, true, false));
    }
}
