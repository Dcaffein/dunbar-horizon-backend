package com.example.DunbarHorizon.social.adapter.out.persistence.friendship.jdbc;

import com.example.DunbarHorizon.account.domain.User;
import com.example.DunbarHorizon.social.application.friendship.dto.FriendView;
import com.example.DunbarHorizon.social.application.friendship.dto.UserProfile;
import com.example.DunbarHorizon.social.domain.friendship.FriendRecognition;
import com.example.DunbarHorizon.social.domain.friendship.Friendship;
import com.example.DunbarHorizon.support.JpaRepositoryTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@JpaRepositoryTest
@Import(FriendshipQueryJdbcAdapter.class)
class FriendshipQueryJdbcAdapterTest {

    private static final LocalDate CREATED_AT = LocalDate.of(2026, 9, 14);
    private static final LocalDateTime INTERACTED_AT = CREATED_AT.atTime(12, 30);

    @Autowired private FriendshipQueryJdbcAdapter friendshipQueryJdbcAdapter;
    @Autowired private TestEntityManager testEntityManager;

    private User low;
    private User high;

    @BeforeEach
    void setUp() {
        low = createUser("low");
        high = createUser("high");
    }

    @Test
    @DisplayName("어느 방향에 있든 내 쪽 설정과 상대 프로필로 친구를 보여 준다")
    void findFriends_ShowsMySettingsAndFriendProfile_FromBothDirections() {
        // given
        Friendship friendship = friendship(low, high);
        testEntityManager.persistAndFlush(friendship);
        testEntityManager.clear();

        // when
        var fromLow = friendshipQueryJdbcAdapter.findFriends(low.getId());
        var fromHigh = friendshipQueryJdbcAdapter.findFriends(high.getId());

        // then
        assertThat(fromLow).containsExactly(new FriendView(high.getId(), "high", "profiles/high", "from low",
                friendship.getIntimacy(), FriendRecognition.normalize(51.0), true, false));
        assertThat(fromHigh).containsExactly(new FriendView(low.getId(), "low", "profiles/low", "from high",
                friendship.getIntimacy(), FriendRecognition.normalize(32.0), false, true));
        assertThat(friendshipQueryJdbcAdapter.findFriend(low.getId(), high.getId())).contains(fromLow.getFirst());
        assertThat(friendshipQueryJdbcAdapter.findFriend(high.getId(), low.getId())).contains(fromHigh.getFirst());
    }

    @Test
    @DisplayName("나나 상대가 비활성이면 친구 목록, 상세, ID 조회에서 제외한다")
    void findFriends_HidesPair_InactiveUser() {
        // given
        testEntityManager.persist(friendship(low, high));
        high.deactivate();
        testEntityManager.flush();
        testEntityManager.clear();

        // when & then
        assertThat(friendshipQueryJdbcAdapter.findFriends(low.getId())).isEmpty();
        assertThat(friendshipQueryJdbcAdapter.findFriends(high.getId())).isEmpty();
        assertThat(friendshipQueryJdbcAdapter.findFriend(low.getId(), high.getId())).isEmpty();
        assertThat(friendshipQueryJdbcAdapter.findFriend(high.getId(), low.getId())).isEmpty();
        assertThat(friendshipQueryJdbcAdapter.findFriendIdsByMuteStatus(low.getId(), true)).isEmpty();
        assertThat(friendshipQueryJdbcAdapter.findFriendIdsByMuteStatus(high.getId(), false)).isEmpty();
        assertThat(friendshipQueryJdbcAdapter.findFriendIdsIn(low.getId(), List.of(high.getId()))).isEmpty();
    }

    @Test
    @DisplayName("관계가 없거나 같은 사용자를 지정하면 상세가 비어 있다")
    void findFriend_ReturnsEmpty_NoPairOrSameUser() {
        // when & then
        assertThat(friendshipQueryJdbcAdapter.findFriend(low.getId(), high.getId())).isEmpty();
        assertThat(friendshipQueryJdbcAdapter.findFriend(low.getId(), low.getId())).isEmpty();
        assertThat(friendshipQueryJdbcAdapter.findFriends(low.getId())).isEmpty();
    }

    @Test
    @DisplayName("사용자가 어느 방향에 속하든 자신의 음소거 설정과 후보 범위를 적용한다")
    void findFriendIds_UsesEachSideAndFiltersCandidates_MixedDirections() {
        // given
        User third = createUser("third");
        User other = createUser("other");
        testEntityManager.persist(friendship(low, high));
        testEntityManager.persistAndFlush(friendship(high, third));

        // when & then
        assertThat(friendshipQueryJdbcAdapter.findFriendIdsByMuteStatus(high.getId(), true)).containsExactly(third.getId());
        assertThat(friendshipQueryJdbcAdapter.findFriendIdsByMuteStatus(high.getId(), false)).containsExactly(low.getId());
        assertThat(friendshipQueryJdbcAdapter.findFriendIdsIn(high.getId(), List.of(low.getId(), third.getId(), other.getId())))
                .containsExactlyInAnyOrder(low.getId(), third.getId());
        assertThat(friendshipQueryJdbcAdapter.findFriendIdsIn(high.getId(), List.of())).isEmpty();
    }

    @Test
    @DisplayName("친구 관계 없이도 대상의 프로필을 조회하고 비활성 또는 없는 계정은 제외한다")
    void findActiveProfile_ReturnsOnlyActiveUserProfile_WithoutFriendship() {
        // given
        high.deactivate();
        testEntityManager.flush();

        // when & then
        assertThat(friendshipQueryJdbcAdapter.findActiveProfile(low.getId()))
                .contains(new UserProfile(low.getId(), "low", "profiles/low"));
        assertThat(friendshipQueryJdbcAdapter.findActiveProfile(high.getId())).isEmpty();
        assertThat(friendshipQueryJdbcAdapter.findActiveProfile(Long.MAX_VALUE)).isEmpty();
    }

    private User createUser(String nickname) {
        return testEntityManager.persistAndFlush(User.builder()
                .email(nickname + "@jdbc-friend-views.test")
                .nickname(nickname)
                .profileImage("profiles/" + nickname)
                .build());
    }

    private static Friendship friendship(User low, User high) {
        return Friendship.restore(CREATED_AT,
                FriendRecognition.restore(high.getId(), "from high", 32.0, INTERACTED_AT.plusDays(1), false, true),
                FriendRecognition.restore(low.getId(), "from low", 51.0, INTERACTED_AT, true, false));
    }
}
