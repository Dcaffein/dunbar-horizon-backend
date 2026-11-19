package com.example.DunbarHorizon.social.adapter.out.persistence.network;

import com.example.DunbarHorizon.account.domain.User;
import com.example.DunbarHorizon.social.adapter.out.persistence.friendship.FriendshipPersistenceAdapter;
import com.example.DunbarHorizon.social.application.network.dto.MutualFriendEdgeResult;
import com.example.DunbarHorizon.social.application.network.dto.NodeEdgeResult;
import com.example.DunbarHorizon.social.application.network.dto.NodeGraphResult;
import com.example.DunbarHorizon.social.application.network.SocialNetworkQueryService;
import com.example.DunbarHorizon.social.domain.network.DunbarCircle;
import com.example.DunbarHorizon.social.domain.friendship.FriendRecognition;
import com.example.DunbarHorizon.social.domain.network.NetworkEdgeLimits;
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
@Import({SocialNetworkJdbcRepository.class, SocialNetworkQueryService.class, FriendshipPersistenceAdapter.class,
        NetworkEdgeLimits.class})
class SocialNetworkJdbcRepositoryTest {

    @Autowired private SocialNetworkJdbcRepository socialNetworkJdbcRepository;
    @Autowired private SocialNetworkQueryService socialNetworkQueryService;
    @Autowired private TestEntityManager em;
    @Autowired private JdbcTemplate jdbc;

    @Test
    @DisplayName("상위 활성 친구만 경계로 삼고 두 ID 방향에서 조회자 관심도를 정규화한다")
    void getDefaultNetworkGraph_SelectsActiveBoundaryAndViewerInterest() {
        User lower = createUser("lower");
        User me = createUser("me");
        User higher = createUser("higher");
        User third = createUser("third");
        User fourth = createUser("fourth");
        User isolated = createUser("isolated");
        User outside = createUser("outside");
        User inactive = createUser("inactive");
        inactive.deactivate();
        em.flush();

        connect(me, lower, 0.9375);
        connect(me, higher, 0.96875, 0.75);
        connect(me, third, 0.75);
        connect(me, fourth, 0.5);
        connect(me, isolated, 0.5);
        connect(me, outside, 0.5);
        connect(me, inactive, 0.96875);
        connect(lower, higher, 0.875);
        connect(lower, third, 0.75);
        connect(lower, fourth, 0.5);
        connect(lower, outside, 0.96875);

        var graph = socialNetworkJdbcRepository.getDefaultNetworkGraph(me.getId(), DunbarCircle.SUPPORT, 1, 2);

        assertThat(graph).containsExactly(
                new NodeGraphResult(lower.getId(), 0.9375,
                        List.of(new NodeEdgeResult(higher.getId(), 0.875), new NodeEdgeResult(third.getId(), 0.75))),
                new NodeGraphResult(higher.getId(), 0.96875, List.of(new NodeEdgeResult(lower.getId(), 0.875))),
                new NodeGraphResult(third.getId(), 0.75, List.of(new NodeEdgeResult(lower.getId(), 0.75))),
                new NodeGraphResult(fourth.getId(), 0.5, List.of(new NodeEdgeResult(lower.getId(), 0.5))),
                new NodeGraphResult(isolated.getId(), 0.5, List.of()));
        assertThat(socialNetworkJdbcRepository.getDefaultNetworkGraph(inactive.getId(), DunbarCircle.DUNBAR, 5, 10)).isEmpty();
    }

    @Test
    @DisplayName("내 라벨에 속한 활성 친구를 먼저 좁힌 뒤 상위 경계를 고른다")
    void getLabelCustomNetwork_FiltersByOwnedLabelBeforeLimit() {
        User member = createUser("member");
        User me = createUser("me");
        User otherMember = createUser("other-member");
        User stranger = createUser("stranger");
        User inactive = createUser("inactive");
        inactive.deactivate();
        em.flush();

        connect(me, member, 0.75);
        connect(me, otherMember, 0.5);
        connect(me, inactive, 0.96875);
        connect(member, otherMember, 0.875);
        for (int i = 0; i < 5; i++) {
            connect(me, createUser("unlabelled-" + i), 0.9375);
        }
        createLabel("mine", me, member, otherMember, stranger, inactive);
        createLabel("theirs", member, me, otherMember);
        createLabel("empty", me);

        assertThat(socialNetworkJdbcRepository.getLabelCustomNetwork(me.getId(), "mine", DunbarCircle.SUPPORT, 5, 10))
                .containsExactly(
                        new NodeGraphResult(member.getId(), 0.75, List.of(new NodeEdgeResult(otherMember.getId(), 0.875))),
                        new NodeGraphResult(otherMember.getId(), 0.5, List.of(new NodeEdgeResult(member.getId(), 0.875))));
        assertThat(socialNetworkQueryService.getLabelNetwork(me.getId(), "theirs")).isEmpty();
        assertThat(socialNetworkQueryService.getLabelNetwork(me.getId(), "empty")).isEmpty();
    }

    @Test
    @DisplayName("실제 활성 공통 친구만 대상 친밀도 순으로 반환하고 2홉 대상은 친밀도를 숨긴다")
    void getNetworkEdges_ValidatesClientIdsAndPreservesExposure() {
        User lower = createUser("lower");
        User me = createUser("me");
        User target = createUser("target");
        User higher = createUser("higher");
        User forged = createUser("forged");
        User inactive = createUser("inactive");
        User omitted = createUser("omitted");
        inactive.deactivate();
        em.flush();

        connect(me, lower, 0.875);
        connect(me, higher, 0.5);
        connect(me, inactive, 0.9375);
        connect(me, omitted, 0.5);
        connect(target, lower, 0.5);
        connect(target, higher, 0.75);
        connect(target, forged, 0.9375);
        connect(target, inactive, 0.9375);
        connect(target, omitted, 0.96875);
        List<Long> candidates = List.of(forged.getId(), higher.getId(), lower.getId(), inactive.getId(), higher.getId());

        assertThat(socialNetworkQueryService.getNetworkEdges(me.getId(), target.getId(), candidates)).containsExactly(
                new MutualFriendEdgeResult(target.getId(), higher.getId(), null),
                new MutualFriendEdgeResult(target.getId(), lower.getId(), null));
        connect(me, target, 0.875);
        assertThat(socialNetworkQueryService.getNetworkEdges(me.getId(), target.getId(), candidates)).containsExactly(
                new MutualFriendEdgeResult(target.getId(), higher.getId(), 0.75),
                new MutualFriendEdgeResult(target.getId(), lower.getId(), 0.5));
        assertThat(socialNetworkJdbcRepository.getDirectFriendEdgesForTarget(me.getId(), target.getId(), candidates, 1))
                .containsExactly(new MutualFriendEdgeResult(target.getId(), higher.getId(), 0.75));

        target.deactivate();
        em.flush();
        assertThat(socialNetworkQueryService.getNetworkEdges(me.getId(), target.getId(), candidates)).isEmpty();
        assertThat(socialNetworkQueryService.getNetworkEdges(me.getId(), target.getId(), List.of())).isEmpty();
    }

    private User createUser(String nickname) {
        return em.persistAndFlush(User.builder().email(nickname + "@mysql-graph.test").nickname(nickname).build());
    }

    private void connect(User from, User to, double normalizedInterest) {
        connect(from, to, normalizedInterest, normalizedInterest);
    }

    private void connect(User from, User to, double fromNormalized, double toNormalized) {
        boolean fromIsLow = from.getId() < to.getId();
        double fromInterest = rawInterest(fromNormalized);
        double toInterest = rawInterest(toNormalized);
        jdbc.update("""
                INSERT INTO friendships (
                    low_user_id, high_user_id, low_interest_score, high_interest_score,
                    low_is_muted, high_is_muted, low_is_routable, high_is_routable
                ) VALUES (?, ?, ?, ?, FALSE, FALSE, FALSE, FALSE)
                """, Math.min(from.getId(), to.getId()), Math.max(from.getId(), to.getId()),
                fromIsLow ? fromInterest : toInterest, fromIsLow ? toInterest : fromInterest);
    }

    private static double rawInterest(double normalized) {
        return FriendRecognition.CONVERGENCE_K * normalized / (1 - normalized);
    }

    private void createLabel(String id, User owner, User... members) {
        jdbc.update("INSERT INTO labels (id, owner_id, name) VALUES (?, ?, ?)", id, owner.getId(), id);
        for (User member : members) {
            jdbc.update("INSERT INTO label_members (label_id, member_id) VALUES (?, ?)", id, member.getId());
        }
    }
}
