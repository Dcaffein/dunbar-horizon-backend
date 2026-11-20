package com.example.DunbarHorizon.social.adapter.out.persistence.network;

import com.example.DunbarHorizon.account.domain.User;
import com.example.DunbarHorizon.buzz.adapter.out.social.AnchorRecipientStrategyAdapter;
import com.example.DunbarHorizon.buzz.application.dto.info.recipient.AnchorRecipientSpec;
import com.example.DunbarHorizon.social.application.network.dto.AnchorExpansionResult;
import com.example.DunbarHorizon.social.application.network.SocialExpansionQueryService;
import com.example.DunbarHorizon.social.domain.friendship.FriendRecognition;
import com.example.DunbarHorizon.social.domain.friendship.exception.FriendshipNotFoundException;
import com.example.DunbarHorizon.support.JpaRepositoryTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

@JpaRepositoryTest
@Import({SocialExpansionJdbcAdapter.class, SocialExpansionQueryService.class, AnchorRecipientStrategyAdapter.class})
class SocialExpansionJdbcAdapterTest {

    @Autowired private SocialExpansionJdbcAdapter socialExpansionJdbcAdapter;
    @Autowired private SocialExpansionQueryService socialExpansionQueryService;
    @Autowired private AnchorRecipientStrategyAdapter anchorRecipientStrategyAdapter;
    @Autowired private TestEntityManager em;
    @Autowired private JdbcTemplate jdbc;

    @Test
    @DisplayName("추천은 활성 외부 후보의 공개 설정과 공통 친구·공유 라벨 점수를 적용하고 친밀도순으로 제한한다")
    void getRecommendedNetworkByAnchor_OrdersQualifiedCandidates_OutsideMyFriends() {
        // given
        User lowCandidate = user("low-candidate");
        User me = user("me");
        User anchor = user("anchor");
        User highCandidate = user("high-candidate");
        User hidden = user("hidden");
        User inactive = user("inactive");
        User insufficient = user("insufficient");
        User mutualOne = user("mutual-one");
        User mutualTwo = user("mutual-two");
        inactive.deactivate();
        em.flush();

        connect(me, anchor, 0.8, true, true);
        connect(me, mutualOne, 0.5, true, true);
        connect(me, mutualTwo, 0.5, true, true);
        connect(anchor, lowCandidate, 0.9, false, true);
        connect(anchor, highCandidate, 0.7, false, true);
        connect(anchor, hidden, 0.99, true, false);
        connect(anchor, inactive, 0.98, false, true);
        connect(anchor, insufficient, 0.97, false, true);
        for (User candidate : new User[]{highCandidate, hidden, inactive}) {
            connect(candidate, mutualOne, 0.5, false, false);
            connect(candidate, mutualTwo, 0.5, false, false);
        }
        connect(lowCandidate, mutualOne, 0.5, false, false);
        connect(insufficient, mutualTwo, 0.5, false, false);
        label("shared", anchor, me, lowCandidate);
        label("different-owner", mutualOne, me, insufficient);
        label("no-viewer", anchor, insufficient);

        // when & then
        assertThat(socialExpansionQueryService.getRecommendationsByAnchor(me.getId(), anchor.getId()))
                .containsExactly(result(lowCandidate), result(highCandidate));
        assertThat(socialExpansionJdbcAdapter.getRecommendedNetworkByAnchor(me.getId(), anchor.getId(), 2, 1))
                .containsExactly(result(lowCandidate));
        assertThat(socialExpansionJdbcAdapter.getRecommendedNetworkByAnchor(me.getId(), anchor.getId(), 3, 10)).isEmpty();
    }

    @Test
    @DisplayName("기준 친구 확장과 Buzz는 외부 후보와 비공개 친구를 제외한 같은 친구 목록을 사용한다")
    void getAnchorExpansion_MatchesBuzzRecipients_InsideMyFriends() {
        // given
        User me = user("me");
        User eligible = user("eligible");
        User anchor = user("anchor");
        User hidden = user("hidden");
        User outside = user("outside");
        connect(me, anchor, 0.8, true, true);
        connect(me, eligible, 0.5, false, false);
        connect(me, hidden, 0.5, false, false);
        connect(anchor, eligible, 0.7, false, true);
        connect(anchor, hidden, 0.9, true, false);
        connect(anchor, outside, 0.99, false, true);
        label("shared", anchor, me, eligible, hidden, outside);

        // when
        var expansion = socialExpansionQueryService.getAnchorExpansion(me.getId(), anchor.getId(), 1.0);
        var recipients = anchorRecipientStrategyAdapter.fetchRecipientIds(me.getId(), new AnchorRecipientSpec(anchor.getId(), 1.0));

        // then
        assertThat(expansion).containsExactly(result(eligible));
        assertThat(recipients).containsExactly(eligible.getId());
        assertThat(socialExpansionJdbcAdapter.getRecommendedNetworkByAnchor(me.getId(), anchor.getId(), 1, 10))
                .containsExactly(result(outside));
    }

    @Test
    @DisplayName("기준 친구 외에 활성 친구가 없으면 빈 결과이며 친구가 아닌 기준 사용자는 거절한다")
    void getRecommendationsByAnchor_HandlesMissingRelationships() {
        // given
        User me = user("me");
        User anchor = user("anchor");
        User outside = user("outside");
        User inactive = user("inactive");
        inactive.deactivate();
        em.flush();
        connect(me, anchor, 0.9, true, true);
        connect(me, inactive, 0.7, true, true);
        connect(anchor, outside, 0.8, true, true);
        connect(outside, inactive, 0.8, true, true);
        label("shared", anchor, me, outside);

        // when & then
        assertThat(socialExpansionJdbcAdapter.getRecommendedNetworkByAnchor(me.getId(), anchor.getId(), 1, 10)).isEmpty();
        assertThat(socialExpansionQueryService.getAnchorExpansion(me.getId(), anchor.getId(), 1.0)).isEmpty();
        assertThatThrownBy(() -> socialExpansionQueryService.getRecommendationsByAnchor(me.getId(), outside.getId()))
                .isInstanceOf(FriendshipNotFoundException.class);
    }

    @Test
    @DisplayName("활성 사용자 쌍의 친밀도만 ID 순서와 무관하게 조회한다")
    void findIntimacy_ReturnsOnlyActivePair_EitherOrder() {
        // given
        User me = user("me");
        User friend = user("friend");
        User inactive = user("inactive");
        User stranger = user("stranger");
        connect(me, friend, 0.9, true, true);
        connect(me, inactive, 0.7, true, true);
        inactive.deactivate();
        em.flush();

        // when & then
        assertThat(socialExpansionJdbcAdapter.findIntimacy(me.getId(), friend.getId()))
                .hasValueSatisfying(intimacy -> assertThat(intimacy).isCloseTo(0.9, within(1e-9)));
        assertThat(socialExpansionJdbcAdapter.findIntimacy(friend.getId(), me.getId()))
                .hasValueSatisfying(intimacy -> assertThat(intimacy).isCloseTo(0.9, within(1e-9)));
        assertThat(socialExpansionJdbcAdapter.findIntimacy(me.getId(), inactive.getId())).isEmpty();
        assertThat(socialExpansionJdbcAdapter.findIntimacy(me.getId(), stranger.getId())).isEmpty();
        assertThat(socialExpansionJdbcAdapter.findIntimacy(me.getId(), me.getId())).isEmpty();
    }

    private User user(String nickname) {
        return em.persistAndFlush(User.builder().email(nickname + "@mysql-anchor.test").nickname(nickname).build());
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

    private void label(String id, User owner, User... members) {
        jdbc.update("INSERT INTO labels (id, owner_id, name) VALUES (?, ?, ?)", id, owner.getId(), id);
        for (User member : members) {
            jdbc.update("INSERT INTO label_members (label_id, member_id) VALUES (?, ?)", id, member.getId());
        }
    }

    private static AnchorExpansionResult result(User user) {
        return new AnchorExpansionResult(user.getId(), user.getNickname());
    }
}
