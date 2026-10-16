package com.example.DunbarHorizon.social.adapter.out;

import com.example.DunbarHorizon.social.adapter.out.persistence.neo4j.FriendshipRepositoryAdapter;
import com.example.DunbarHorizon.social.adapter.out.persistence.neo4j.springData.FriendshipNeo4jRepository;
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


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Neo4jRepositoryTest
@Import(FriendshipRepositoryAdapter.class)
class FriendshipRepositoryAdapterTest {

    @Autowired
    private FriendshipRepositoryAdapter friendshipRepository;

    @Autowired
    private FriendshipNeo4jRepository friendshipNeo4jRepository;

    @Autowired
    private SocialUserNeo4jRepository socialUserNeo4jRepository;

    @Autowired
    private Neo4jClient neo4jClient;

    private SocialUser userA;
    private SocialUser userB;

    @BeforeEach
    void setUp() {
        neo4jClient.query("MATCH (n) DETACH DELETE n").run();
        userA = socialUserNeo4jRepository.save(new SocialUser(1L, "사용자A", "url1"));
        userB = socialUserNeo4jRepository.save(new SocialUser(2L, "사용자B", "url2"));
    }

    @Test
    @DisplayName("생성한 Friendship의 노드·관계 속성이 도메인 계산 결과와 같다")
    void create_도메인_계산값으로_저장된다() {
        // given
        Friendship friendship = FriendTestFactory.createFriendship(userA, userB);

        // when
        friendshipRepository.create(friendship);

        // then
        Friendship found = friendshipNeo4jRepository.findById(friendship.getId()).orElseThrow();
        assertThat(found.getIntimacy()).isEqualTo(friendship.getIntimacy());
        assertThat(found.getCreatedAt()).isEqualTo(friendship.getCreatedAt());
        assertThat(found.getRecognitions())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id", "user")
                .containsExactlyInAnyOrderElementsOf(friendship.getRecognitions());
        assertThat(found.getRecognitions())
                .extracting(recognition -> recognition.getUser().getId())
                .containsExactlyInAnyOrder(userA.getId(), userB.getId());
    }

    @Test
    @DisplayName("같은 Friendship을 두 번 생성해도 노드와 관계가 하나씩만 남는다")
    void create_중복_호출시_멱등하다() {
        // given
        Friendship friendship = FriendTestFactory.createFriendship(userA, userB);
        friendshipRepository.create(friendship);

        // when
        friendshipRepository.create(friendship);

        // then
        assertThat(count("MATCH (f:Friendship) RETURN count(f)")).isEqualTo(1L);
        assertThat(count("MATCH ()-[r:HAS_FRIENDSHIP]->() RETURN count(r)")).isEqualTo(2L);
    }

    @Test
    @DisplayName("한쪽이 비활성 사용자면 UserReferenceNotFoundException이 발생하고 Friendship이 생성되지 않는다")
    void create_비활성_사용자면_예외() {
        // given
        Friendship friendship = FriendTestFactory.createFriendship(userA, userB);
        neo4jClient.query("MATCH (u:SocialUser {id: 2}) REMOVE u:UserReference SET u:InactiveSocialUser").run();

        // when & then
        assertThatThrownBy(() -> friendshipRepository.create(friendship))
                .isInstanceOf(UserReferenceNotFoundException.class);
        assertThat(count("MATCH (f:Friendship) RETURN count(f)")).isZero();
    }

    private Long count(String cypher) {
        return neo4jClient.query(cypher).fetchAs(Long.class).one().orElseThrow();
    }
}
