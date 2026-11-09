package com.example.DunbarHorizon.social.adapter.out;

import com.example.DunbarHorizon.social.adapter.out.persistence.neo4j.FriendshipRepositoryAdapter;
import com.example.DunbarHorizon.social.adapter.out.persistence.neo4j.springData.SocialUserNeo4jRepository;
import com.example.DunbarHorizon.social.domain.friend.FriendTestFactory;
import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.social.domain.friend.FriendshipDecayPolicy;
import com.example.DunbarHorizon.social.domain.socialUser.SocialUser;
import com.example.DunbarHorizon.support.Neo4jRepositoryTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.neo4j.core.Neo4jClient;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * 감쇄 규칙의 기준은 Friendship.decay이고, applyDecay 쿼리는 그 규칙을 관계 전체에 대량으로 실행하는 구현이다.
 * 같은 입력에서 두 결과가 같은지 확인해 쿼리가 도메인 규칙에서 벗어나지 않게 지킨다.
 */
@Neo4jRepositoryTest
@Import(FriendshipRepositoryAdapter.class)
class FriendshipDecayConsistencyTest {

    private static final double TOLERANCE = 1e-9;
    private static final LocalDateTime DECAY_TIME = LocalDateTime.of(2026, 10, 1, 0, 0);
    private static final LocalDateTime STALE = DECAY_TIME.minusDays(10);
    private static final LocalDateTime RECENT = DECAY_TIME.plusDays(10);

    private final FriendshipDecayPolicy policy = new FriendshipDecayPolicy();

    @Autowired
    private FriendshipRepositoryAdapter friendshipRepository;

    @Autowired
    private SocialUserNeo4jRepository socialUserNeo4jRepository;

    @Autowired
    private Neo4jClient neo4jClient;

    @BeforeEach
    void setUp() {
        neo4jClient.query("MATCH (n) DETACH DELETE n").run();
        SocialUser userA = socialUserNeo4jRepository.save(new SocialUser(1L, "사용자A", "url1"));
        SocialUser userB = socialUserNeo4jRepository.save(new SocialUser(2L, "사용자B", "url2"));
        friendshipRepository.create(FriendTestFactory.createFriendship(userA, userB));
    }

    @Test
    @DisplayName("양쪽 모두 감쇄 대상이면 쿼리와 도메인 메서드의 결과가 같다")
    void applyDecay_matchesDomain_bothStale() {
        assertQueryMatchesDomain(40.0, STALE, 20.0, STALE, 0.5);
    }

    @Test
    @DisplayName("한쪽만 감쇄 대상이면 쿼리와 도메인 메서드의 결과가 같다")
    void applyDecay_matchesDomain_oneSideStale() {
        assertQueryMatchesDomain(40.0, STALE, 20.0, RECENT, 0.5);
    }

    @Test
    @DisplayName("감쇄하면 하한 아래로 떨어지는 경우 쿼리와 도메인 메서드의 결과가 같다")
    void applyDecay_matchesDomain_fallsBelowThreshold() {
        assertQueryMatchesDomain(policy.getMinThreshold() * 1.01, STALE, 20.0, STALE, 0.5);
    }

    @Test
    @DisplayName("이미 하한 이하인 경우 쿼리와 도메인 메서드의 결과가 같다")
    void applyDecay_matchesDomain_alreadyAtOrBelowThreshold() {
        assertQueryMatchesDomain(0.5, STALE, policy.getMinThreshold(), STALE, 0.9);
    }

    @Test
    @DisplayName("양쪽 모두 감쇄 대상이 아니면 쿼리와 도메인 메서드의 결과가 같다")
    void applyDecay_matchesDomain_bothRecent() {
        assertQueryMatchesDomain(40.0, RECENT, 20.0, RECENT, 0.9);
    }

    private void assertQueryMatchesDomain(double lowScore, LocalDateTime lowAt,
                                          double highScore, LocalDateTime highAt, double storedIntimacy) {
        // given
        seed(lowScore, lowAt, highScore, highAt, storedIntimacy);
        Friendship expected = friendshipRepository.findById("1_2").orElseThrow();
        double rate = policy.getDecayRate();
        double threshold = policy.getMinThreshold();

        // when
        friendshipRepository.applyDecay(rate, threshold, DECAY_TIME);
        expected.decay(rate, threshold, DECAY_TIME);

        // then
        Friendship actual = friendshipRepository.findById("1_2").orElseThrow();
        assertThat(actual.getMyInterestScore(1L)).isCloseTo(expected.getMyInterestScore(1L), within(TOLERANCE));
        assertThat(actual.getMyInterestScore(2L)).isCloseTo(expected.getMyInterestScore(2L), within(TOLERANCE));
        assertThat(actual.getIntimacy()).isCloseTo(expected.getIntimacy(), within(TOLERANCE));
    }

    private void seed(double lowScore, LocalDateTime lowAt, double highScore, LocalDateTime highAt, double intimacy) {
        neo4jClient.query("""
                        MATCH ()-[r:FRIENDS_WITH {id: '1_2'}]->()
                        SET r += $properties
                        """)
                .bind(Map.of(
                        "lowInterestScore", lowScore,
                        "lowLastInteractedAt", lowAt,
                        "highInterestScore", highScore,
                        "highLastInteractedAt", highAt,
                        "intimacy", intimacy
                )).to("properties")
                .run();
    }
}
