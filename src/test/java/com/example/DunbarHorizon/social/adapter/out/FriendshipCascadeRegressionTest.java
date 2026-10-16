package com.example.DunbarHorizon.social.adapter.out;

import com.example.DunbarHorizon.social.adapter.out.persistence.neo4j.FriendshipRepositoryAdapter;
import com.example.DunbarHorizon.social.adapter.out.persistence.neo4j.springData.SocialUserNeo4jRepository;
import com.example.DunbarHorizon.social.domain.friend.FriendTestFactory;
import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.social.domain.socialUser.SocialUser;
import com.example.DunbarHorizon.social.domain.socialUser.exception.UserReferenceNotFoundException;
import com.example.DunbarHorizon.support.Neo4jRepositoryTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.neo4j.core.Neo4jClient;

import java.time.LocalDateTime;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SocialUser 스냅샷을 읽은 뒤 그 노드가 바뀌어도, 묵은 스냅샷으로 Friendship을 쓰면서 노드를 덮어쓰지 않는지 검증한다.
 */
@Neo4jRepositoryTest
@Import(FriendshipRepositoryAdapter.class)
class FriendshipCascadeRegressionTest {

    private static final LocalDateTime UPDATED_AT = LocalDateTime.of(2026, 10, 14, 14, 55, 5);

    @Autowired
    private FriendshipRepositoryAdapter friendshipRepository;

    @Autowired
    private SocialUserNeo4jRepository socialUserNeo4jRepository;

    @Autowired
    private Neo4jClient neo4jClient;

    private SocialUser staleA;
    private SocialUser staleB;

    @BeforeEach
    void setUp() {
        neo4jClient.query("MATCH (n) DETACH DELETE n").run();
        socialUserNeo4jRepository.save(new SocialUser(1L, "사용자A", "oldImageA"));
        socialUserNeo4jRepository.save(new SocialUser(2L, "사용자B", "oldImage"));
        staleA = socialUserNeo4jRepository.findById(1L).orElseThrow();
        staleB = socialUserNeo4jRepository.findById(2L).orElseThrow();
    }

    @Test
    @DisplayName("스냅샷을 읽은 뒤 비활성화된 사용자는 Friendship 생성 시도 후에도 InactiveSocialUser로 남는다")
    void 비활성화_후_생성해도_라벨이_되살아나지_않는다() {
        // given
        Friendship friendship = FriendTestFactory.createFriendship(staleA, staleB);
        neo4jClient.query("MATCH (u:SocialUser {id: 2}) REMOVE u:UserReference SET u:InactiveSocialUser").run();

        // when
        assertThatThrownBy(() -> friendshipRepository.create(friendship))
                .isInstanceOf(UserReferenceNotFoundException.class);

        // then
        assertThat(labelsOf(2L)).containsExactlyInAnyOrder("SocialUser", "InactiveSocialUser");
    }

    @Test
    @DisplayName("스냅샷을 읽은 뒤 갱신된 프로필은 Friendship 생성 후에도 되돌아가지 않는다")
    void 프로필_갱신_후_생성해도_속성이_되돌아가지_않는다() {
        // given
        Friendship friendship = FriendTestFactory.createFriendship(staleA, staleB);
        neo4jClient.query("MATCH (u:SocialUser {id: 2}) " +
                        "SET u.nickname = '새닉네임', u.profileImageUrl = 'newImage', u.updatedAt = $updatedAt")
                .bind(UPDATED_AT).to("updatedAt")
                .run();

        // when
        friendshipRepository.create(friendship);

        // then
        SocialUser userB = socialUserNeo4jRepository.findById(2L).orElseThrow();
        assertThat(userB.getNickname()).isEqualTo("새닉네임");
        assertThat(userB.getProfileImageUrl()).isEqualTo("newImage");
        assertThat(userB.getUpdatedAt()).isEqualTo(UPDATED_AT);
        assertThat(labelsOf(2L)).containsExactlyInAnyOrder("SocialUser", "UserReference");
    }

    @SuppressWarnings("unchecked")
    private Collection<String> labelsOf(Long userId) {
        return (Collection<String>) neo4jClient.query("MATCH (u:SocialUser {id: $id}) RETURN labels(u) AS labels")
                .bind(userId).to("id")
                .fetch().one().orElseThrow()
                .get("labels");
    }
}
