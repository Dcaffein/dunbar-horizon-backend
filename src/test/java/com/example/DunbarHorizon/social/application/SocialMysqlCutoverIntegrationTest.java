package com.example.DunbarHorizon.social.application;

import com.example.DunbarHorizon.global.event.interaction.BatchMutualInteractionEvent;
import com.example.DunbarHorizon.global.event.interaction.InteractionType;
import com.example.DunbarHorizon.global.event.interaction.ScoreDirection;
import com.example.DunbarHorizon.social.application.network.dto.IntermediaryExpansionResult;
import com.example.DunbarHorizon.social.application.network.dto.ConnectionPathResult.IntermediaryResult;
import com.example.DunbarHorizon.social.application.label.dto.LabelMemberResult;
import com.example.DunbarHorizon.social.application.network.dto.NodeGraphResult;
import com.example.DunbarHorizon.social.application.friendrequest.FriendRequestService;
import com.example.DunbarHorizon.social.application.friendship.FriendshipCommandService;
import com.example.DunbarHorizon.social.application.friendship.FriendshipQueryService;
import com.example.DunbarHorizon.social.application.label.LabelQueryService;
import com.example.DunbarHorizon.social.application.network.SocialConnectionPathQueryService;
import com.example.DunbarHorizon.social.application.network.SocialExpansionQueryService;
import com.example.DunbarHorizon.social.application.network.SocialNetworkQueryService;
import com.example.DunbarHorizon.social.domain.network.DunbarCircle;
import com.example.DunbarHorizon.social.domain.friendship.FriendRecognition;
import com.example.DunbarHorizon.social.domain.friendship.FriendRequestStatus;
import com.example.DunbarHorizon.social.domain.friendship.InteractionScores;
import com.example.DunbarHorizon.support.MySqlTestContainerConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=cutover-test",
        "spring.security.oauth2.client.registration.google.client-secret=cutover-test",
        "spring.mail.username=cutover-test",
        "spring.mail.password=cutover-test"
})
@ActiveProfiles("test")
@Import(MySqlTestContainerConfig.class)
@Testcontainers
class SocialMysqlCutoverIntegrationTest {

    @Container
    @ServiceConnection
    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7.0");

    private static final long ME = 148_001L;
    private static final long INTERMEDIARY = 148_002L;
    private static final long FRIEND = 148_003L;
    private static final long TARGET = 148_004L;
    private static final long REQUESTER = 148_005L;
    private static final String LABEL_ID = "cutover-intermediary-label";

    @Autowired private ApplicationContext app;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private TransactionTemplate transactions;
    @Autowired private FriendshipQueryService friendshipQueryService;
    @Autowired private FriendshipCommandService friendshipCommandService;
    @Autowired private FriendRequestService friendRequestService;
    @Autowired private SocialNetworkQueryService socialNetworkQueryService;
    @Autowired private SocialExpansionQueryService socialExpansionQueryService;
    @Autowired private SocialConnectionPathQueryService socialConnectionPathQueryService;
    @Autowired private LabelQueryService labelQueryService;

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM label_members WHERE label_id = ?", LABEL_ID);
        jdbc.update("DELETE FROM labels WHERE id = ?", LABEL_ID);
        jdbc.update("DELETE FROM friend_requests WHERE requester_id = ?", REQUESTER);
        jdbc.update("DELETE FROM friendships WHERE low_user_id BETWEEN ? AND ?", ME, REQUESTER);
        jdbc.update("DELETE FROM users WHERE user_id BETWEEN ? AND ?", ME, REQUESTER);
    }

    @Test
    @DisplayName("Neo4j 연결 없이 앱이 기동하고 이관 형식의 데이터에서 Social 읽기와 쓰기가 동작한다")
    void appUsesMySqlForSocial_AfterCutover() {
        // given
        for (long userId : List.of(ME, INTERMEDIARY, FRIEND, TARGET, REQUESTER)) {
            jdbc.update("INSERT INTO users (user_id, email, nickname, role, status) VALUES (?, ?, ?, 'USER', 'ACTIVE')",
                    userId, userId + "@cutover.test", "user-" + userId);
        }
        insertFriendship(ME, INTERMEDIARY, 0.8);
        insertFriendship(ME, FRIEND, 0.6);
        insertFriendship(INTERMEDIARY, FRIEND, 0.7);
        insertFriendship(INTERMEDIARY, TARGET, 0.6);
        insertFriendship(FRIEND, TARGET, 0.5);
        jdbc.update("INSERT INTO labels (id, owner_id, name) VALUES (?, ?, '동네')", LABEL_ID, INTERMEDIARY);
        jdbc.update("INSERT INTO label_members (label_id, member_id) VALUES (?, ?), (?, ?)", LABEL_ID, ME, LABEL_ID, TARGET);
        jdbc.update("""
                INSERT INTO friend_requests (requester_id, receiver_id, created_at, status)
                VALUES (?, ?, CURRENT_TIMESTAMP, 'PENDING')
                """, REQUESTER, ME);

        // when & then
        assertThat(app.containsBean("neo4jDriver")).isFalse();
        assertThat(socialNetworkQueryService.getFriendsNetwork(ME, DunbarCircle.DUNBAR)).extracting(NodeGraphResult::nodeId)
                .containsExactly(INTERMEDIARY, FRIEND);
        assertThat(socialNetworkQueryService.getLabelNetwork(INTERMEDIARY, LABEL_ID)).extracting(NodeGraphResult::nodeId)
                .containsExactly(ME, TARGET);
        assertThat(socialExpansionQueryService.getRecommendationsByIntermediary(ME, INTERMEDIARY)).extracting(IntermediaryExpansionResult::id)
                .containsExactly(TARGET);
        assertThat(socialConnectionPathQueryService.getConnectionPath(ME, TARGET).intermediaries()).extracting(IntermediaryResult::userId)
                .containsExactly(INTERMEDIARY, FRIEND);
        assertThat(friendshipQueryService.getFriendIdsIn(ME, List.of(FRIEND, TARGET))).containsExactly(FRIEND);
        assertThat(friendshipQueryService.getMutedIds(ME)).isEmpty();
        assertThat(friendshipQueryService.getListenableFriendIds(ME)).containsExactlyInAnyOrder(INTERMEDIARY, FRIEND);

        jdbc.update("UPDATE users SET nickname = 'renamed' WHERE user_id = ?", TARGET);
        assertThat(friendshipQueryService.getSocialProfile(TARGET).nickname()).isEqualTo("renamed");
        assertThat(labelQueryService.getLabelMembers(INTERMEDIARY, LABEL_ID)).contains(new LabelMemberResult(TARGET, "renamed"));
        jdbc.update("UPDATE users SET status = 'DORMANT' WHERE user_id = ?", TARGET);
        assertThat(socialExpansionQueryService.getRecommendationsByIntermediary(ME, INTERMEDIARY)).isEmpty();
        jdbc.update("UPDATE users SET status = 'ACTIVE' WHERE user_id = ?", TARGET);

        friendRequestService.updateStatus(ME, REQUESTER, FriendRequestStatus.ACCEPTED);
        assertThat(friendshipQueryService.areFriends(ME, REQUESTER)).isTrue();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM friend_requests WHERE requester_id = ?", Integer.class,
                REQUESTER)).isZero();

        transactions.executeWithoutResult(status -> app.publishEvent(new BatchMutualInteractionEvent(
                148L, List.of(FRIEND), ME, InteractionType.FLAG_ENDED, ScoreDirection.GRANT)));
        assertThat(friendshipQueryService.getFriend(ME, FRIEND).myInterestScore())
                .isEqualTo(FriendRecognition.normalize(scoreFor(0.6) + InteractionScores.scoreOf(InteractionType.FLAG_ENDED)));

        assertThat(labelQueryService.getMemberIdsInOwnedLabels(INTERMEDIARY, List.of(LABEL_ID))).containsExactlyInAnyOrder(ME, TARGET);
        friendshipCommandService.endFriendship(ME, INTERMEDIARY);
        assertThat(friendshipQueryService.areFriends(ME, INTERMEDIARY)).isFalse();
        assertThat(labelQueryService.getMemberIdsInOwnedLabels(INTERMEDIARY, List.of(LABEL_ID))).containsExactly(TARGET);
    }

    private void insertFriendship(long low, long high, double intimacy) {
        jdbc.update("""
                INSERT INTO friendships (low_user_id, high_user_id, created_at,
                    low_interest_score, low_is_muted, low_is_routable,
                    high_interest_score, high_is_muted, high_is_routable)
                VALUES (?, ?, CURRENT_DATE, ?, FALSE, TRUE, ?, FALSE, TRUE)
                """, low, high, scoreFor(intimacy), scoreFor(intimacy));
    }

    private static double scoreFor(double intimacy) {
        return FriendRecognition.CONVERGENCE_K * intimacy / (1 - intimacy);
    }
}
