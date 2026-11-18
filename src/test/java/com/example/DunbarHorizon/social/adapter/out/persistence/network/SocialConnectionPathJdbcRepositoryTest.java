package com.example.DunbarHorizon.social.adapter.out.persistence.network;

import com.example.DunbarHorizon.account.domain.User;
import com.example.DunbarHorizon.social.adapter.out.persistence.friendship.FriendshipPersistenceAdapter;
import com.example.DunbarHorizon.social.application.dto.result.ConnectionPathResult.IntermediaryResult;
import com.example.DunbarHorizon.social.application.dto.result.ConnectionPathResult;
import com.example.DunbarHorizon.social.application.service.SocialConnectionPathQueryService;
import com.example.DunbarHorizon.social.domain.friend.FriendRecognition;
import com.example.DunbarHorizon.support.JpaRepositoryTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@JpaRepositoryTest
@Import({SocialConnectionPathJdbcRepository.class, SocialConnectionPathQueryService.class,
        FriendshipPersistenceAdapter.class})
class SocialConnectionPathJdbcRepositoryTest {

    @Autowired private SocialConnectionPathJdbcRepository repository;
    @Autowired private SocialConnectionPathQueryService queryService;
    @Autowired private TestEntityManager em;
    @Autowired private JdbcTemplate jdbc;

    @Test
    @DisplayName("이미 친구인 대상에게도 친밀도 곱 상위 3명과 공개 가능한 중개인 전체 수를 반환한다")
    void getConnectionPath_ReturnsRankedIntermediaries_ForExistingFriend() {
        // given
        User lower = user("lower");
        User me = user("me");
        User target = user("target");
        User higher = user("higher");
        User tie = user("tie");
        User last = user("last");
        connect(me, target, 0.9, false, false);
        connect(me, lower, 0.5, false, true);
        connect(lower, target, 0.8, false, true);
        connect(me, higher, 0.8, false, true);
        connect(higher, target, 0.5, false, true);
        connect(me, tie, 0.75, false, true);
        connect(tie, target, 0.75, false, true);
        connect(me, last, 0.99, false, true);
        connect(last, target, 0.2, false, true);

        // when
        var result = queryService.getConnectionPath(me.getId(), target.getId());

        // then
        assertThat(result).isEqualTo(new ConnectionPathResult(true, 4,
                List.of(result(tie), result(lower), result(higher))));
        assertThat(repository.findIntermediaries(me.getId(), target.getId(), 0))
                .isEqualTo(new ConnectionPathResult.Intermediaries(List.of(), 4));
    }

    @Test
    @DisplayName("ID 방향과 무관하게 중개인과 대상이 비공개로 설정한 경로를 전체 수에서도 제외한다")
    void findIntermediaries_FiltersEachUsersSetting_InBothIdDirections() {
        // given
        User lowHiddenByMid = user("low-mid-hidden");
        User lowHiddenByTarget = user("low-target-hidden");
        User me = user("me");
        User target = user("target");
        User highHiddenByMid = user("high-mid-hidden");
        User highHiddenByTarget = user("high-target-hidden");
        User open = user("open");
        User inactive = user("inactive");
        inactive.deactivate();
        em.flush();

        for (User mid : List.of(lowHiddenByMid, highHiddenByMid)) {
            connect(me, mid, 0.99, true, false);
            connect(mid, target, 0.99, false, true);
        }
        for (User mid : List.of(lowHiddenByTarget, highHiddenByTarget)) {
            connect(me, mid, 0.99, false, true);
            connect(mid, target, 0.99, true, false);
        }
        connect(me, inactive, 0.99, true, true);
        connect(inactive, target, 0.99, true, true);
        connect(me, open, 0.5, false, true);
        connect(open, target, 0.6, false, true);

        // when & then
        assertThat(queryService.getConnectionPath(me.getId(), target.getId()))
                .isEqualTo(new ConnectionPathResult(false, 1, List.of(result(open))));
        target.deactivate();
        em.flush();
        assertThat(repository.findIntermediaries(me.getId(), target.getId(), 3))
                .isEqualTo(new ConnectionPathResult.Intermediaries(List.of(), 0));
    }

    @Test
    @DisplayName("공통 친구가 없거나 자신을 조회하면 중개인 없이 반환한다")
    void getConnectionPath_ReturnsEmpty_WithoutSharedFriendsOrForSelf() {
        // given
        User me = user("me");
        User friend = user("friend");
        User target = user("target");
        connect(me, friend, 0.8, true, true);

        // when & then
        var empty = new ConnectionPathResult(false, 0, List.of());
        assertThat(queryService.getConnectionPath(me.getId(), target.getId())).isEqualTo(empty);
        assertThat(queryService.getConnectionPath(me.getId(), me.getId())).isEqualTo(empty);
    }

    private User user(String nickname) {
        return em.persistAndFlush(User.builder().email(nickname + "@mysql-path.test").nickname(nickname).build());
    }

    private void connect(User from, User to, double intimacy, boolean fromRoutable, boolean toRoutable) {
        boolean fromIsLow = from.getId() < to.getId();
        jdbc.update("""
                INSERT INTO friendships (
                    low_user_id, high_user_id, low_interest_score, high_interest_score,
                    low_is_muted, high_is_muted, low_is_routable, high_is_routable
                ) VALUES (?, ?, ?, ?, FALSE, FALSE, ?, ?)
                """, Math.min(from.getId(), to.getId()), Math.max(from.getId(), to.getId()),
                scoreFor(intimacy), scoreFor(intimacy),
                fromIsLow ? fromRoutable : toRoutable, fromIsLow ? toRoutable : fromRoutable);
    }

    private static double scoreFor(double intimacy) {
        return FriendRecognition.CONVERGENCE_K * intimacy / (1 - intimacy);
    }

    private static IntermediaryResult result(User user) {
        return new IntermediaryResult(user.getId(), user.getNickname());
    }
}
