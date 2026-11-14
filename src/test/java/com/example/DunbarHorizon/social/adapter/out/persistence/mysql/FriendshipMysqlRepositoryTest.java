package com.example.DunbarHorizon.social.adapter.out.persistence.mysql;

import com.example.DunbarHorizon.account.domain.User;
import com.example.DunbarHorizon.social.domain.friend.FriendRecognition;
import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.social.domain.socialUser.UserReference;
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
            assertThat(friendship.getId()).isEqualTo(original.getId());
            assertThat(friendship.getCreatedAt()).isEqualTo(CREATED_AT);
            assertThat(friendship.getIntimacy()).isEqualTo(INTIMACY);
            assertThat(friendship.getFriend(low.getId()).getNickname()).isEqualTo("high");
            assertThat(friendship.getFriend(low.getId()).getProfileImageUrl()).isEqualTo("profiles/high");
            assertThat(friendship.getFriendAlias(low.getId())).isEqualTo("from low");
            assertThat(friendship.getMyInterestScore(low.getId())).isEqualTo(51.0);
            assertThat(friendship.isMuted(low.getId())).isTrue();
            assertThat(friendship.isRoutable(low.getId())).isFalse();
        });
        assertThat(fromHigh).singleElement().satisfies(friendship -> {
            assertThat(friendship.getId()).isEqualTo(original.getId());
            assertThat(friendship.getIntimacy()).isEqualTo(INTIMACY);
            assertThat(friendship.getFriend(high.getId()).getId()).isEqualTo(low.getId());
            assertThat(friendship.getFriendAlias(high.getId())).isEqualTo("from high");
            assertThat(friendship.getMyInterestScore(high.getId())).isEqualTo(32.0);
            assertThat(friendship.isMuted(high.getId())).isFalse();
            assertThat(friendship.isRoutable(high.getId())).isTrue();
        });
        assertThat(repository.existsFriendshipBetween(high.getId(), low.getId())).isTrue();
    }

    @Test
    @DisplayName("같은 관계를 다시 생성해도 한 행만 남고 저장된 값이 바뀌지 않는다")
    void create_PreservesExistingRow_DuplicatePair() {
        // given
        Friendship original = friendship(low, high);
        Friendship duplicate = friendship(low, high);
        duplicate.updateFriendAlias(low.getId(), "overwritten");

        // when
        repository.create(original);
        repository.create(duplicate);

        // then
        assertThat(repository.findByUserId(low.getId())).singleElement()
                .satisfies(friendship -> assertThat(friendship.getFriendAlias(low.getId())).isEqualTo("from low"));
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
    @DisplayName("친구 설정 갱신은 요청한 쪽만 바꾸고 점수와 친밀도를 유지한다")
    void updateUserFields_PreservesOtherSideAndScores_HighUser() {
        // given
        Friendship original = friendship(low, high);
        repository.create(original);
        Friendship locked = repository.findByIdForUpdate(original.getId()).orElseThrow();

        // when
        locked.updateUserFields(high.getId(), "", true, false);
        repository.updateUserFields(locked, high.getId());

        // then
        Friendship updated = repository.findById(original.getId()).orElseThrow();
        assertThat(updated.getFriendAlias(high.getId())).isEmpty();
        assertThat(updated.isMuted(high.getId())).isTrue();
        assertThat(updated.isRoutable(high.getId())).isFalse();
        assertThat(updated.getFriendAlias(low.getId())).isEqualTo("from low");
        assertThat(updated.isMuted(low.getId())).isTrue();
        assertThat(updated.isRoutable(low.getId())).isFalse();
        assertThat(updated.getIntimacy()).isEqualTo(INTIMACY);
        assertThat(updated.getRecognitions()).extracting(FriendRecognition::getInterestScore)
                .containsExactly(51.0, 32.0);
        assertThat(updated.getRecognitions()).extracting(FriendRecognition::getLastInteractedAt)
                .containsExactly(INTERACTED_AT, INTERACTED_AT.plusDays(1));
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
    @DisplayName("관계 한 행을 삭제하면 어느 방향에서도 조회되지 않는다")
    void delete_RemovesPair_FromBothDirections() {
        // given
        Friendship original = friendship(low, high);
        repository.create(original);

        // when
        repository.delete(original.getId());

        // then
        assertThat(repository.findById(original.getId())).isEmpty();
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
        return Friendship.restore(Friendship.generateCompositeId(low.getId(), high.getId()), CREATED_AT, INTIMACY,
                List.of(FriendRecognition.restore(new Reference(high.getId()), "from high", 32.0,
                                INTERACTED_AT.plusDays(1), false, true),
                        FriendRecognition.restore(new Reference(low.getId()), "from low", 51.0,
                                INTERACTED_AT, true, false)));
    }

    private record Reference(Long getId) implements UserReference {
        @Override
        public String getNickname() {
            return null;
        }

        @Override
        public String getProfileImageUrl() {
            return null;
        }
    }
}
