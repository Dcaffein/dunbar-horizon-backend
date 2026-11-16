package com.example.DunbarHorizon.social.adapter.out.persistence.mysql;

import com.example.DunbarHorizon.account.domain.User;
import com.example.DunbarHorizon.social.adapter.out.persistence.jpa.FriendshipRepositoryAdapter;
import com.example.DunbarHorizon.social.domain.friend.FriendRecognition;
import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.support.JpaRepositoryTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@JpaRepositoryTest
@Import({FriendshipMysqlRepository.class, FriendshipRepositoryAdapter.class, SocialUserMysqlRepository.class})
class FriendshipMysqlRepositoryTest {

    private static final LocalDate CREATED_AT = LocalDate.of(2026, 9, 14);
    private static final LocalDateTime INTERACTED_AT = CREATED_AT.atTime(12, 30);

    @Autowired private FriendshipMysqlRepository repository;
    @Autowired private FriendshipRepositoryAdapter friendshipRepository;
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
    @DisplayName("사용자가 어느 방향에 속하든 자신의 음소거 설정과 후보 범위를 적용한다")
    void findFriendIds_UsesEachSideAndFiltersCandidates_MixedDirections() {
        // given
        User third = createUser("third");
        User other = createUser("other");
        friendshipRepository.create(friendship(low, high));
        friendshipRepository.create(friendship(high, third));

        // when & then
        assertThat(repository.findFriendIdsByMuteStatus(high.getId(), true)).containsExactly(third.getId());
        assertThat(repository.findFriendIdsByMuteStatus(high.getId(), false)).containsExactly(low.getId());
        assertThat(repository.filterFriendIdsAmong(high.getId(), List.of(low.getId(), third.getId(), other.getId())))
                .containsExactlyInAnyOrder(low.getId(), third.getId());
        assertThat(repository.filterFriendIdsAmong(high.getId(), Set.of())).isEmpty();
    }

    @Test
    @DisplayName("계정이 비활성화되면 ID 조회와 잠금에서 관계가 없는 것으로 다뤄지고 행은 남는다")
    void lookups_TreatAsMissing_InactiveAccount() {
        // given
        friendshipRepository.create(friendship(low, high));
        high.deactivate();
        em.flush();
        em.clear();
        Long me = low.getId();
        Long friend = high.getId();

        // when & then
        assertThat(repository.findFriendIdsByMuteStatus(me, true)).isEmpty();
        assertThat(repository.findFriendIdsByMuteStatus(friend, false)).isEmpty();
        assertThat(repository.filterFriendIdsAmong(me, List.of(friend))).isEmpty();
        assertThat(repository.findBetweenForUpdate(me, friend)).isEmpty();
        assertThat(repository.existsFriendshipForUpdate(me, friend)).isFalse();
        assertThat(repository.findAllBetweenForUpdate(me, List.of(friend))).isEmpty();
        assertThat(repository.findAllAmongForUpdate(List.of(me, friend))).isEmpty();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM friendships WHERE low_user_id = ? AND high_user_id = ?",
                Integer.class, me, friend)).isEqualTo(1);
    }

    @Test
    @DisplayName("같은 사용자 두 명으로 조회하면 예외 없이 관계 없음으로 끝난다")
    void lookups_ReturnNothing_SameUser() {
        // given
        friendshipRepository.create(friendship(low, high));
        Long self = low.getId();

        // when & then
        assertThat(repository.findBetweenForUpdate(self, self)).isEmpty();
        assertThat(repository.existsFriendshipForUpdate(self, self)).isFalse();
        assertThat(repository.findAllBetweenForUpdate(self, List.of(self))).isEmpty();
        assertThat(repository.findAllAmongForUpdate(List.of(self, self))).isEmpty();
    }

    @Test
    @DisplayName("한 사람과 여러 사람 사이의 잠금 조회는 실제 친구 관계만 돌려준다")
    void findAllBetweenForUpdate_ReturnsExistingFriendships_MixedCandidates() {
        // given
        User third = createUser("third");
        friendshipRepository.create(friendship(low, high));

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
        friendshipRepository.create(friendship(high, third));
        friendshipRepository.create(friendship(low, high));
        friendshipRepository.create(friendship(low, outsider));

        // when
        var locked = repository.findAllAmongForUpdate(List.of(third.getId(), low.getId(), high.getId(), low.getId()));

        // then
        assertThat(locked).extracting(friendship -> friendship.getLower().getOwnerId() + "_"
                        + friendship.getHigher().getOwnerId())
                .containsExactly(low.getId() + "_" + high.getId(), high.getId() + "_" + third.getId());
    }

    private User createUser(String nickname) {
        return em.persistAndFlush(User.builder()
                .email(nickname + "@mysql-friends.test")
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
