package com.example.DunbarHorizon.social.adapter.out.persistence.mysql;

import com.example.DunbarHorizon.account.domain.User;
import com.example.DunbarHorizon.social.domain.friend.FriendRecognition;
import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.social.domain.friend.exception.AlreadyFriendsException;
import com.example.DunbarHorizon.social.domain.friend.exception.FriendshipNotFoundException;
import com.example.DunbarHorizon.support.JpaRepositoryTest;
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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@JpaRepositoryTest
@Import({FriendshipMysqlRepository.class, SocialUserMysqlRepository.class})
class FriendshipMysqlRepositoryTest {

    private static final LocalDate CREATED_AT = LocalDate.of(2026, 9, 14);
    private static final LocalDateTime INTERACTED_AT = CREATED_AT.atTime(12, 30);
    private static final double INTIMACY = 0.45;

    @Autowired private FriendshipMysqlRepository repository;
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
    void findByUserId_ReturnsSameRelationship_FromBothDirections() {
        // given
        Friendship original = friendship(low, high);
        repository.create(original);

        // when
        var fromLow = repository.findByUserId(low.getId());
        var fromHigh = repository.findByUserId(high.getId());

        // then
        assertThat(fromLow).singleElement().satisfies(friendship -> {
            assertThat(friendship.getUserIds()).containsExactlyInAnyOrder(low.getId(), high.getId());
            assertThat(friendship.getFriendId(low.getId())).isEqualTo(high.getId());
            assertThat(friendship.getCreatedAt()).isEqualTo(CREATED_AT);
            assertThat(friendship.getIntimacy()).isEqualTo(INTIMACY);
            assertThat(friendship.getFriendAlias(low.getId())).isEqualTo("from low");
            assertThat(friendship.getMyInterestScore(low.getId())).isEqualTo(51.0);
            assertThat(friendship.isMuted(low.getId())).isTrue();
            assertThat(friendship.isRoutable(low.getId())).isFalse();
        });
        assertThat(fromHigh).singleElement().satisfies(friendship -> {
            assertThat(friendship.getUserIds()).containsExactlyInAnyOrder(low.getId(), high.getId());
            assertThat(friendship.getIntimacy()).isEqualTo(INTIMACY);
            assertThat(friendship.getFriendId(high.getId())).isEqualTo(low.getId());
            assertThat(friendship.getFriendAlias(high.getId())).isEqualTo("from high");
            assertThat(friendship.getMyInterestScore(high.getId())).isEqualTo(32.0);
            assertThat(friendship.isMuted(high.getId())).isFalse();
            assertThat(friendship.isRoutable(high.getId())).isTrue();
        });
        assertThat(repository.existsFriendshipBetween(high.getId(), low.getId())).isTrue();
    }

    @Test
    @DisplayName("이미 친구인 쌍을 다시 생성하면 친구 중복 예외로 끝나고 기존 행의 설정과 점수는 그대로다")
    void create_ThrowsAlreadyFriends_DuplicatePair() {
        // given
        repository.create(friendship(low, high));
        Friendship duplicate = friendship(high, low);
        duplicate.updateUserFields(low.getId(), "overwritten", false, true);
        duplicate.adjustMutualInterestScore(10);

        // when & then
        assertThatThrownBy(() -> repository.create(duplicate)).isInstanceOf(AlreadyFriendsException.class);
        assertThat(repository.findByUserId(low.getId())).singleElement().satisfies(friendship -> {
            assertThat(friendship.getFriendAlias(low.getId())).isEqualTo("from low");
            assertThat(friendship.isMuted(low.getId())).isTrue();
            assertThat(friendship.getMyInterestScore(low.getId())).isEqualTo(51.0);
            assertThat(friendship.getMyInterestScore(high.getId())).isEqualTo(32.0);
        });
    }

    @Test
    @DisplayName("DB는 사용자 ID 순서가 뒤집힌 관계를 저장하지 않는다")
    void insert_RejectsReversedPair_CheckConstraint() {
        // when & then
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO friendships (
                    low_user_id, high_user_id, intimacy,
                    low_interest_score, low_is_muted, low_is_routable,
                    high_interest_score, high_is_muted, high_is_routable
                ) VALUES (?, ?, 0.45, 51, TRUE, FALSE, 32, FALSE, TRUE)
                """, high.getId(), low.getId()))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("chk_friendships_user_order");
    }

    @Test
    @DisplayName("친구 설정 저장은 본인 쪽 설정 열만 쓰고 로드 뒤 바뀐 상대 설정과 점수를 보존한다")
    void updateUserFields_WritesOnlyOwnSettings_OtherColumnsChangedAfterLoad() {
        // given
        Friendship original = friendship(low, high);
        repository.create(original);
        Friendship loaded = repository.findBetween(low.getId(), high.getId()).orElseThrow();
        jdbc.update("""
                UPDATE friendships SET low_friend_alias = 'changed by low', low_is_muted = FALSE,
                    low_interest_score = 60, high_interest_score = 40
                WHERE low_user_id = ? AND high_user_id = ?
                """, low.getId(), high.getId());
        loaded.updateUserFields(high.getId(), "", true, false);

        // when
        repository.updateUserFields(loaded, high.getId());

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
                .containsExactly(INTERACTED_AT, INTERACTED_AT.plusDays(1));
        assertThat(jdbc.queryForObject("SELECT high_friend_alias FROM friendships WHERE low_user_id = ?",
                String.class, low.getId())).isNull();
    }

    @Test
    @DisplayName("바뀐 설정이 없어도 존재하는 관계의 설정 저장은 성공한다")
    void updateUserFields_Succeeds_UnchangedSettings() {
        // given
        Friendship original = friendship(low, high);
        repository.create(original);
        Friendship loaded = repository.findBetween(low.getId(), high.getId()).orElseThrow();

        // when
        repository.updateUserFields(loaded, low.getId());

        // then
        assertThat(repository.findBetween(low.getId(), high.getId()).orElseThrow().getFriendAlias(low.getId()))
                .isEqualTo("from low");
    }

    @Test
    @DisplayName("로드한 뒤 관계가 삭제되면 설정 저장은 관계 없음 예외로 끝난다")
    void updateUserFields_ThrowsNotFound_DeletedAfterLoad() {
        // given
        Friendship original = friendship(low, high);
        repository.create(original);
        Friendship loaded = repository.findBetween(low.getId(), high.getId()).orElseThrow();
        repository.deleteAll(List.of(loaded));

        // when & then
        assertThatThrownBy(() -> repository.updateUserFields(loaded, low.getId()))
                .isInstanceOf(FriendshipNotFoundException.class);
    }

    @Test
    @DisplayName("별명이 없는 관계는 별명 열을 NULL로 저장한다")
    void create_StoresNullAlias_EmptyAlias() {
        // given
        Friendship friendship = Friendship.restore(CREATED_AT, INTIMACY,
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
    @DisplayName("사용자가 어느 방향에 속하든 자신의 음소거 설정과 후보 범위를 적용한다")
    void findFriendIds_UsesEachSideAndFiltersCandidates_MixedDirections() {
        // given
        User third = createUser("third");
        User other = createUser("other");
        repository.create(friendship(low, high));
        repository.create(friendship(high, third));

        // when & then
        assertThat(repository.findFriendIdsByMuteStatus(high.getId(), true)).containsExactly(third.getId());
        assertThat(repository.findFriendIdsByMuteStatus(high.getId(), false)).containsExactly(low.getId());
        assertThat(repository.filterFriendIdsAmong(high.getId(), List.of(low.getId(), third.getId(), other.getId())))
                .containsExactlyInAnyOrder(low.getId(), third.getId());
        assertThat(repository.filterFriendIdsAmong(high.getId(), Set.of())).isEmpty();
    }

    @Test
    @DisplayName("계정이 비활성화되면 친구 목록과 후보 ID에서 제외된다")
    void findFriends_ExcludesInactiveAccount_AfterStatusChange() {
        // given
        repository.create(friendship(low, high));
        high.deactivate();
        em.flush();
        em.clear();

        // when & then
        assertThat(repository.findByUserId(low.getId())).isEmpty();
        assertThat(repository.findFriendIdsByMuteStatus(low.getId(), true)).isEmpty();
        assertThat(repository.filterFriendIdsAmong(low.getId(), List.of(high.getId()))).isEmpty();
    }

    @Test
    @DisplayName("같은 사용자 두 명으로 조회하면 예외 없이 관계 없음으로 끝난다")
    void lookups_ReturnNothing_SameUser() {
        // given
        repository.create(friendship(low, high));
        Long self = low.getId();

        // when & then
        assertThat(repository.findBetween(self, self)).isEmpty();
        assertThat(repository.findBetweenForUpdate(self, self)).isEmpty();
        assertThat(repository.existsFriendshipBetween(self, self)).isFalse();
        assertThat(repository.existsFriendshipForUpdate(self, self)).isFalse();
        assertThat(repository.findAllBetweenForUpdate(self, List.of(self))).isEmpty();
        assertThat(repository.findAllAmongForUpdate(List.of(self, self))).isEmpty();
    }

    @Test
    @DisplayName("한 사람과 여러 사람 사이의 잠금 조회는 실제 친구 관계만 돌려준다")
    void findAllBetweenForUpdate_ReturnsExistingFriendships_MixedCandidates() {
        // given
        User third = createUser("third");
        repository.create(friendship(low, high));

        // when
        var locked = repository.findAllBetweenForUpdate(high.getId(), List.of(low.getId(), third.getId(), high.getId()));

        // then
        assertThat(locked).singleElement()
                .satisfies(friendship -> assertThat(friendship.getFriendId(high.getId())).isEqualTo(low.getId()));
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

    @Test
    @DisplayName("아카이브 대상 잠금 조회는 친밀도 열이 기준 이하인 관계만 돌려준다")
    void findArchivableForUpdate_ReturnsRelationshipsAtOrBelowThreshold() {
        // given
        User third = createUser("third");
        repository.create(friendship(low, high));
        repository.create(Friendship.restore(CREATED_AT, 0.01,
                FriendRecognition.restore(high.getId(), null, 0.5, INTERACTED_AT, false, true),
                FriendRecognition.restore(third.getId(), null, 0.5, INTERACTED_AT, false, true)));

        // when
        var locked = repository.findArchivableForUpdate(0.02);

        // then
        assertThat(locked).singleElement()
                .satisfies(friendship -> assertThat(friendship.getUserIds())
                        .containsExactlyInAnyOrder(high.getId(), third.getId()));
    }

    @Test
    @DisplayName("관계를 삭제하면 어느 방향에서도 조회되지 않는다")
    void deleteAll_RemovesPair_FromBothDirections() {
        // given
        Friendship original = friendship(low, high);
        repository.create(original);

        // when
        repository.deleteAll(List.of(repository.findBetween(high.getId(), low.getId()).orElseThrow()));

        // then
        assertThat(repository.findBetween(low.getId(), high.getId())).isEmpty();
        assertThat(repository.existsFriendshipBetween(high.getId(), low.getId())).isFalse();
        assertThat(repository.findByUserId(low.getId())).isEmpty();
        assertThat(repository.findByUserId(high.getId())).isEmpty();
    }

    private User createUser(String nickname) {
        return em.persistAndFlush(User.builder()
                .email(nickname + "@mysql-friends.test")
                .nickname(nickname)
                .profileImage("profiles/" + nickname)
                .build());
    }

    private static Friendship friendship(User low, User high) {
        return Friendship.restore(CREATED_AT, INTIMACY,
                FriendRecognition.restore(high.getId(), "from high", 32.0,
                        INTERACTED_AT.plusDays(1), false, true),
                FriendRecognition.restore(low.getId(), "from low", 51.0,
                        INTERACTED_AT, true, false));
    }

}
