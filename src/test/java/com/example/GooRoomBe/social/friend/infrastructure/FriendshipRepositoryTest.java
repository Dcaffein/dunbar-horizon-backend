package com.example.GooRoomBe.social.friend.infrastructure;

import com.example.GooRoomBe.social.friend.domain.Friendship;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.neo4j.DataNeo4jTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.example.GooRoomBe.social.common.SocialSchemaConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DataNeo4jTest
@Testcontainers
@ActiveProfiles("test")
class FriendshipRepositoryTest {

    @Container
    @ServiceConnection
    static Neo4jContainer<?> neo4jContainer = new Neo4jContainer<>("neo4j:5");

    @Autowired
    private FriendshipRepository friendshipRepository;

    @Autowired
    private Neo4jClient neo4jClient;

    private final String userAId = "userA";
    private final String userBId = "userB";
    private final String userCId = "userC";

    @BeforeEach
    void setUp() {
        // 모든 데이터 초기화
        friendshipRepository.deleteAll();
        neo4jClient.query("MATCH (n) DETACH DELETE n").run();

        // 기본 데이터 셋업 (A-B는 친구, C는 남남)
        createBaseData();
    }

    /**
     * 기본 테스트 데이터 생성 (A-B 친구 관계)
     */
    private void createBaseData() {
        String cypher = String.format("""
            CREATE (a:%s {id: $idA}), (b:%s {id: $idB}), (c:%s {id: $idC})
            CREATE (fs:%s {id: 'fs_AB'})
            CREATE (a)-[:%s]->(fs)<-[:%s]-(b)
            """,
                SOCIAL_USER, SOCIAL_USER, SOCIAL_USER,
                FRIENDSHIP,
                MEMBER_OF, MEMBER_OF
        );

        neo4jClient.query(cypher)
                .bindAll(Map.of("idA", userAId, "idB", userBId, "idC", userCId))
                .run();
    }

    @Test
    @DisplayName("exists: 친구 관계가 존재하면 true를 반환한다")
    void existsFriendshipBetween_ShouldReturnTrue_WhenExists() {
        // when
        boolean exists = friendshipRepository.existsFriendshipBetween(userAId, userBId);

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("exists: 친구 관계가 없으면 false를 반환한다")
    void existsFriendshipBetween_ShouldReturnFalse_WhenNotExists() {
        // when
        boolean exists = friendshipRepository.existsFriendshipBetween(userAId, userCId);

        // then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("find: 친구 ID로 Friendship 엔티티를 조회한다")
    void findFriendshipByUsers_ShouldReturnEntity() {
        // when
        Optional<Friendship> result = friendshipRepository.findFriendshipByUsers(userAId, userBId);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("fs_AB");
    }

    @Test
    @DisplayName("filter: ID 리스트 중 실제 친구 관계인 것만 필터링한다")
    void filterFriendsFromIdList_ShouldReturnOnlyFriends() {
        // given
        List<String> candidates = List.of(userBId, userCId, "unknownUser");

        // when
        Set<Friendship> result = friendshipRepository.filterFriendsFromIdList(userAId, candidates);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.iterator().next().getId()).isEqualTo("fs_AB");
    }

    @Test
    @DisplayName("updateAlias: 별명을 수정하고 저장하면 DB의 엣지 속성(Edge Property)이 업데이트된다")
    void updateFriendAlias_ShouldPersistEdgeProperties() {
        // Given: 별명 테스트를 위한 전용 데이터 셋업
        friendshipRepository.deleteAll();
        neo4jClient.query("MATCH (n) DETACH DELETE n").run();

        String initCypher = String.format("""
            CREATE (a:%s {id: $idA}), (b:%s {id: $idB})
            CREATE (fs:%s {id: $fsId})
            CREATE (a)-[:%s {friendAlias: '초기별명', onIntroduce: false}]->(fs)
            CREATE (b)-[:%s]->(fs)
            """,
                SOCIAL_USER, SOCIAL_USER,
                FRIENDSHIP,
                MEMBER_OF, MEMBER_OF
        );

        neo4jClient.query(initCypher)
                .bindAll(Map.of("idA", userAId, "idB", userBId, "fsId", "fs_AB"))
                .run();

        // When: JPA(SDN) 로직 수행
        Friendship friendship = friendshipRepository.findById("fs_AB").orElseThrow();
        friendship.updateFriendAlias(userAId, "찐친");
        friendshipRepository.save(friendship);

        // Then: DB 직접 조회 검증
        String verifyCypher = String.format("""
                MATCH (u:%s {id: $uid})-[r:%s]->(fs:%s {id: $fsId})
                RETURN r.friendAlias
                """,
                SOCIAL_USER, MEMBER_OF, FRIENDSHIP
        );

        String savedAlias = neo4jClient.query(verifyCypher)
                .bindAll(Map.of("uid", userAId, "fsId", "fs_AB"))
                .fetchAs(String.class)
                .one()
                .orElseThrow();

        assertThat(savedAlias).isEqualTo("찐친");
    }

    // 👇 [신규 추가] 감쇠(Decay) 및 친밀도 재계산 로직 검증 👇

    @Test
    @DisplayName("applyDecay: 점수를 감쇠시키고 친밀도(Intimacy)를 재계산해야 한다")
    void applyDecay_ShouldWorkCorrectly() {
        // Given: A와 B가 서로 100점인 상태를 DB에 직접 생성
        // Sqrt(100 * 100) = 100 (초기 상태)
        String setupCypher = String.format("""
            CREATE (a:%s {id: $idA}), (b:%s {id: $idB})
            CREATE (fs:%s {id: 'fs_decay_test', intimacy: 100.0})
            CREATE (a)-[:%s {interestScore: 100.0}]->(fs)
            CREATE (b)-[:%s {interestScore: 100.0}]->(fs)
            """, SOCIAL_USER, SOCIAL_USER, FRIENDSHIP, MEMBER_OF, MEMBER_OF);

        neo4jClient.query(setupCypher)
                .bindAll(Map.of("idA", userAId, "idB", userBId))
                .run();

        // When: 감쇠율 0.9 (10% 감소), 임계값 0.1
        friendshipRepository.applyDecayToAllFriendships(0.9, 0.1);

        // Then: 결과 조회 및 검증
        Friendship updated = friendshipRepository.findById("fs_decay_test").orElseThrow();

        // 예상: 100 * 0.9 = 90
        // Intimacy: Sqrt(90 * 90) = 90
        assertThat(updated.getIntimacy()).isCloseTo(90.0, within(0.001));
    }

    @Test
    @DisplayName("applyDecay: 점수가 임계값(Threshold) 미만으로 떨어지면 0점이 되고, 친밀도도 0이 된다")
    void applyDecay_Threshold_ShouldResetToZero() {
        // Given: A=100 (높음), B=10 (곧 0점이 될 운명)
        String setupCypher = String.format("""
            CREATE (a:%s {id: $idA}), (b:%s {id: $idB})
            CREATE (fs:%s {id: 'fs_threshold_test', intimacy: 31.62}) 
            CREATE (a)-[:%s {interestScore: 100.0}]->(fs)
            CREATE (b)-[:%s {interestScore: 10.0}]->(fs)
            """, SOCIAL_USER, SOCIAL_USER, FRIENDSHIP, MEMBER_OF, MEMBER_OF);

        neo4jClient.query(setupCypher)
                .bindAll(Map.of("idA", userAId, "idB", userBId))
                .run();

        // When: 강력한 감쇠 (0.001배), 임계값 0.1
        // B의 예상 점수: 10 * 0.001 = 0.01 -> 임계값(0.1)보다 작으므로 0.0이 됨
        friendshipRepository.applyDecayToAllFriendships(0.001, 0.1);

        // Then
        Friendship updated = friendshipRepository.findById("fs_threshold_test").orElseThrow();

        // A는 점수가 남았지만, B가 0점이 되었으므로
        // 기하 평균 Sqrt(A * 0) = 0
        assertThat(updated.getIntimacy()).isEqualTo(0.0);
    }
}