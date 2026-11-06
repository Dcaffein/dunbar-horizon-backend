package com.example.DunbarHorizon.social.adapter.out;

import com.example.DunbarHorizon.social.adapter.out.persistence.neo4j.FriendshipRepositoryAdapter;
import com.example.DunbarHorizon.social.adapter.out.persistence.neo4j.springData.SocialUserNeo4jRepository;
import com.example.DunbarHorizon.social.domain.friend.FriendTestFactory;
import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.social.domain.friend.FriendshipArchiveCandidate;
import com.example.DunbarHorizon.social.domain.socialUser.SocialUser;
import com.example.DunbarHorizon.social.domain.socialUser.exception.UserReferenceNotFoundException;
import com.example.DunbarHorizon.support.Neo4jRepositoryTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.neo4j.core.Neo4jClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Neo4jRepositoryTest
@Import(FriendshipRepositoryAdapter.class)
class FriendshipRepositoryAdapterTest {

    @Autowired
    private FriendshipRepositoryAdapter friendshipRepository;

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
    @DisplayName("생성한 Friendship의 관계 속성이 도메인 계산 결과와 같다")
    void create_도메인_계산값으로_저장된다() {
        // given
        Friendship friendship = FriendTestFactory.createFriendship(userA, userB);

        // when
        friendshipRepository.create(friendship);

        // then
        Friendship found = friendshipRepository.findById(friendship.getId()).orElseThrow();
        assertThat(found.getIntimacy()).isEqualTo(friendship.getIntimacy());
        assertThat(found.getCreatedAt()).isEqualTo(friendship.getCreatedAt());
        assertThat(found.getRecognitions())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("user")
                .containsExactlyInAnyOrderElementsOf(friendship.getRecognitions());
        assertThat(found.getRecognitions())
                .extracting(recognition -> recognition.getUser().getId())
                .containsExactlyInAnyOrder(userA.getId(), userB.getId());
    }

    @Test
    @DisplayName("같은 Friendship을 두 번 생성해도 FRIENDS_WITH 관계가 하나만 남는다")
    void create_중복_호출시_멱등하다() {
        // given
        Friendship friendship = FriendTestFactory.createFriendship(userA, userB);
        friendshipRepository.create(friendship);

        // when
        friendshipRepository.create(friendship);

        // then
        assertThat(count("MATCH ()-[r:FRIENDS_WITH]->() RETURN count(r)")).isEqualTo(1L);
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
        assertThat(count("MATCH ()-[r:FRIENDS_WITH]->() RETURN count(r)")).isZero();
    }

    @Test
    @DisplayName("낮은 id의 사용자가 시작 노드이고 사용자별 값이 접두사에 맞게 저장된다")
    void create_낮은_id가_시작_노드다() {
        // given
        Friendship friendship = FriendTestFactory.createFriendship(userB, userA);
        friendship.updateUserFields(userB.getId(), "B의 별명", null, null);

        // when
        friendshipRepository.create(friendship);

        // then
        assertThat(count("MATCH (:SocialUser {id: 1})-[r:FRIENDS_WITH {highFriendAlias: 'B의 별명'}]->(:SocialUser {id: 2}) " +
                "RETURN count(r)")).isEqualTo(1L);
    }

    @Test
    @DisplayName("deleteAllByIds는 지정한 친구 관계만 지운다")
    void deleteAllByIds_지정한_관계만_지운다() {
        // given
        SocialUser userC = socialUserNeo4jRepository.save(new SocialUser(3L, "사용자C", "url3"));
        Friendship friendshipAB = FriendTestFactory.createFriendship(userA, userB);
        Friendship friendshipAC = FriendTestFactory.createFriendship(userA, userC);
        friendshipRepository.create(friendshipAB);
        friendshipRepository.create(friendshipAC);

        // when
        friendshipRepository.deleteAllByIds(List.of(friendshipAB.getId()));

        // then
        assertThat(friendshipRepository.existsFriendshipBetween(1L, 2L)).isFalse();
        assertThat(friendshipRepository.existsFriendshipBetween(1L, 3L)).isTrue();
    }

    @Test
    @DisplayName("친밀도가 임계치 이하인 친구 관계를 아카이브 후보로 돌려준다")
    void findArchiveCandidates_임계치_이하만_돌려준다() {
        // given
        Friendship friendship = FriendTestFactory.createFriendship(userA, userB);
        friendshipRepository.create(friendship);

        // when
        List<FriendshipArchiveCandidate> candidates = friendshipRepository.findArchiveCandidates(1.0);
        List<FriendshipArchiveCandidate> none = friendshipRepository.findArchiveCandidates(0.0);

        // then
        assertThat(candidates).containsExactly(new FriendshipArchiveCandidate(
                friendship.getId(), userA.getId(), userB.getId(), friendship.getCreatedAt()));
        assertThat(none).isEmpty();
    }

    private Long count(String cypher) {
        return neo4jClient.query(cypher).fetchAs(Long.class).one().orElseThrow();
    }
}
