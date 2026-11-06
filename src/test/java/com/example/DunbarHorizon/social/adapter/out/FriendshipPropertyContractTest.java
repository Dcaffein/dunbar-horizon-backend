package com.example.DunbarHorizon.social.adapter.out;

import com.example.DunbarHorizon.social.adapter.out.persistence.neo4j.FriendshipRepositoryAdapter;
import com.example.DunbarHorizon.social.adapter.out.persistence.neo4j.springData.SocialUserNeo4jRepository;
import com.example.DunbarHorizon.social.domain.friend.FriendTestFactory;
import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.social.domain.friend.MutualInterestScoreUpdate;
import com.example.DunbarHorizon.social.domain.socialUser.SocialUser;
import com.example.DunbarHorizon.support.Neo4jRepositoryTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.neo4j.core.Neo4jClient;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 쿼리의 속성 이름은 문자열이라 하나를 놓쳐도 에러 없이 다른 키에 쓴다.
 * 모든 쓰기 경로 뒤에 관계의 키가 이 목록과 정확히 같은지 본다.
 */
@Neo4jRepositoryTest
@Import(FriendshipRepositoryAdapter.class)
class FriendshipPropertyContractTest {

    private static final List<String> FRIENDS_WITH_KEYS = List.of(
            "id", "createdAt", "intimacy",
            "lowFriendAlias", "lowInterestScore", "lowLastInteractedAt", "lowIsMuted", "lowIsRoutable",
            "highFriendAlias", "highInterestScore", "highLastInteractedAt", "highIsMuted", "highIsRoutable"
    );

    @Autowired
    private FriendshipRepositoryAdapter friendshipRepository;

    @Autowired
    private SocialUserNeo4jRepository socialUserNeo4jRepository;

    @Autowired
    private Neo4jClient neo4jClient;

    private SocialUser userA;
    private SocialUser userB;
    private Friendship friendship;

    @BeforeEach
    void setUp() {
        neo4jClient.query("MATCH (n) DETACH DELETE n").run();
        userA = socialUserNeo4jRepository.save(new SocialUser(1L, "사용자A", "url1"));
        userB = socialUserNeo4jRepository.save(new SocialUser(2L, "사용자B", "url2"));
        friendship = FriendTestFactory.createFriendship(userA, userB);
        friendship.updateUserFields(userA.getId(), "A의 별명", null, null);
        friendship.updateUserFields(userB.getId(), "B의 별명", null, null);
        friendshipRepository.create(friendship);
    }

    @Test
    @DisplayName("생성한 관계의 속성 키가 계약과 같다")
    void create_keepsContract() {
        assertThat(keys()).containsExactlyInAnyOrderElementsOf(FRIENDS_WITH_KEYS);
    }

    @Test
    @DisplayName("낮은 id 사용자의 단방향 증분 뒤에도 속성 키가 계약과 같다")
    void incrementInterestScore_lowSide_keepsContract() {
        friendshipRepository.incrementInterestScore(friendship.getId(), userA.getId(), userB.getId(), 1.0);

        assertThat(keys()).containsExactlyInAnyOrderElementsOf(FRIENDS_WITH_KEYS);
    }

    @Test
    @DisplayName("높은 id 사용자의 단방향 증분 뒤에도 속성 키가 계약과 같다")
    void incrementInterestScore_highSide_keepsContract() {
        friendshipRepository.incrementInterestScore(friendship.getId(), userB.getId(), userA.getId(), 1.0);

        assertThat(keys()).containsExactlyInAnyOrderElementsOf(FRIENDS_WITH_KEYS);
    }

    @Test
    @DisplayName("상호 증분 뒤에도 속성 키가 계약과 같다")
    void incrementMutualInterestScore_keepsContract() {
        friendshipRepository.incrementMutualInterestScore(friendship.getId(), userA.getId(), userB.getId(), 1.0);

        assertThat(keys()).containsExactlyInAnyOrderElementsOf(FRIENDS_WITH_KEYS);
    }

    @Test
    @DisplayName("배치 증분 뒤에도 속성 키가 계약과 같다")
    void incrementMutualInterestScoresBatch_keepsContract() {
        friendshipRepository.incrementMutualInterestScoresBatch(List.of(
                new MutualInterestScoreUpdate(friendship.getId(), userA.getId(), userB.getId(), 1.0)
        ));

        assertThat(keys()).containsExactlyInAnyOrderElementsOf(FRIENDS_WITH_KEYS);
    }

    @Test
    @DisplayName("낮은 id 사용자의 설정 변경 뒤에도 속성 키가 계약과 같다")
    void updateUserFields_lowSide_keepsContract() {
        friendship.updateUserFields(userA.getId(), "새 별명", true, false);

        friendshipRepository.updateUserFields(friendship, userA.getId());

        assertThat(keys()).containsExactlyInAnyOrderElementsOf(FRIENDS_WITH_KEYS);
    }

    @Test
    @DisplayName("높은 id 사용자의 설정 변경 뒤에도 속성 키가 계약과 같다")
    void updateUserFields_highSide_keepsContract() {
        friendship.updateUserFields(userB.getId(), "새 별명", true, false);

        friendshipRepository.updateUserFields(friendship, userB.getId());

        assertThat(keys()).containsExactlyInAnyOrderElementsOf(FRIENDS_WITH_KEYS);
    }

    @Test
    @DisplayName("감쇄 뒤에도 속성 키가 계약과 같다")
    void applyDecay_keepsContract() {
        friendshipRepository.applyDecay(0.5, 1.0, LocalDateTime.now().plusSeconds(1));

        assertThat(keys()).containsExactlyInAnyOrderElementsOf(FRIENDS_WITH_KEYS);
    }

    @SuppressWarnings("unchecked")
    private Collection<String> keys() {
        return (Collection<String>) neo4jClient.query("MATCH ()-[r:FRIENDS_WITH]->() RETURN keys(r) AS keys")
                .fetch().one().orElseThrow()
                .get("keys");
    }
}
