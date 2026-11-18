package com.example.DunbarHorizon.social.adapter.out.persistence.jdbc;

import com.example.DunbarHorizon.account.domain.User;
import com.example.DunbarHorizon.social.application.dto.FriendView;
import com.example.DunbarHorizon.social.domain.friend.FriendRecognition;
import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.support.JpaRepositoryTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@JpaRepositoryTest
@Import(FriendViewJdbcRepository.class)
class FriendViewJdbcRepositoryTest {

    private static final LocalDate CREATED_AT = LocalDate.of(2026, 9, 14);
    private static final LocalDateTime INTERACTED_AT = CREATED_AT.atTime(12, 30);

    @Autowired private FriendViewJdbcRepository friendViewJdbcRepository;
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
        var fromLow = friendViewJdbcRepository.findFriends(low.getId());
        var fromHigh = friendViewJdbcRepository.findFriends(high.getId());

        // then
        assertThat(fromLow).containsExactly(new FriendView(high.getId(), "high", "profiles/high", "from low",
                friendship.getIntimacy(), FriendRecognition.normalize(51.0), true, false));
        assertThat(fromHigh).containsExactly(new FriendView(low.getId(), "low", "profiles/low", "from high",
                friendship.getIntimacy(), FriendRecognition.normalize(32.0), false, true));
        assertThat(friendViewJdbcRepository.findFriend(low.getId(), high.getId())).contains(fromLow.getFirst());
        assertThat(friendViewJdbcRepository.findFriend(high.getId(), low.getId())).contains(fromHigh.getFirst());
    }

    @Test
    @DisplayName("나나 상대가 비활성이면 친구 목록과 상세에서 보이지 않는다")
    void findFriends_HidesPair_InactiveUser() {
        // given
        testEntityManager.persist(friendship(low, high));
        high.deactivate();
        testEntityManager.flush();
        testEntityManager.clear();

        // when & then
        assertThat(friendViewJdbcRepository.findFriends(low.getId())).isEmpty();
        assertThat(friendViewJdbcRepository.findFriends(high.getId())).isEmpty();
        assertThat(friendViewJdbcRepository.findFriend(low.getId(), high.getId())).isEmpty();
        assertThat(friendViewJdbcRepository.findFriend(high.getId(), low.getId())).isEmpty();
    }

    @Test
    @DisplayName("관계가 없거나 같은 사용자를 지정하면 상세가 비어 있다")
    void findFriend_ReturnsEmpty_NoPairOrSameUser() {
        // when & then
        assertThat(friendViewJdbcRepository.findFriend(low.getId(), high.getId())).isEmpty();
        assertThat(friendViewJdbcRepository.findFriend(low.getId(), low.getId())).isEmpty();
        assertThat(friendViewJdbcRepository.findFriends(low.getId())).isEmpty();
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
