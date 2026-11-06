package com.example.DunbarHorizon.social.adapter.out;

import ac.simons.neo4j.migrations.core.Migrations;
import ac.simons.neo4j.migrations.core.MigrationsConfig;
import com.example.DunbarHorizon.support.Neo4jRepositoryTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 이관은 별도 세션에서 커밋된 데이터를 읽으므로 테스트 트랜잭션을 끈다.
 */
@Neo4jRepositoryTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class FriendshipGraphMigrationTest {

    private static final LocalDate CREATED_AT = LocalDate.of(2026, 1, 2);
    private static final LocalDateTime LOW_INTERACTED_AT = LocalDateTime.of(2026, 3, 4, 5, 6, 7);
    private static final LocalDateTime HIGH_INTERACTED_AT = LocalDateTime.of(2026, 5, 6, 7, 8, 9);

    @Autowired
    private Driver driver;

    @Autowired
    private Neo4jClient neo4jClient;

    private Migrations migrations;

    @BeforeEach
    void setUp() {
        clean();
        migrations = new Migrations(MigrationsConfig.builder()
                .withLocationsToScan("classpath:neo4j/migrations")
                .build(), driver);

        neo4jClient.query("""
                CREATE (u1:SocialUser:UserReference {id: 1})
                CREATE (u2:SocialUser:UserReference {id: 2})
                CREATE (u3:SocialUser:InactiveSocialUser {id: 3})

                CREATE (u2)-[:HAS_FRIENDSHIP {
                    friendAlias: '높은쪽별명', interestScore: 70.0, lastInteractedAt: $highInteractedAt,
                    isMuted: true, isRoutable: false
                }]->(f12:Friendship {id: '1_2', createdAt: $createdAt, intimacy: 0.5})
                CREATE (u1)-[:HAS_FRIENDSHIP {
                    friendAlias: '낮은쪽별명', interestScore: 30.0, lastInteractedAt: $lowInteractedAt,
                    isMuted: false, isRoutable: true
                }]->(f12)

                CREATE (u1)-[:HAS_FRIENDSHIP {interestScore: 10.0, isMuted: false, isRoutable: true}]
                       ->(f13:Friendship {id: '1_3', createdAt: $createdAt, intimacy: 0.2})
                CREATE (u3)-[:HAS_FRIENDSHIP {interestScore: 20.0, isMuted: false, isRoutable: true}]->(f13)
                """)
                .bindAll(Map.of(
                        "createdAt", CREATED_AT,
                        "lowInteractedAt", LOW_INTERACTED_AT,
                        "highInteractedAt", HIGH_INTERACTED_AT
                ))
                .run();
    }

    @AfterEach
    void tearDown() {
        clean();
    }

    @Test
    @DisplayName("이관하면 낮은 id가 시작 노드이고 사용자별 값이 각자의 접두사로 옮겨진다")
    void apply_copiesValuesBySide() {
        // when
        migrations.apply();

        // then
        Map<String, Object> friendship = neo4jClient.query("""
                        MATCH (:SocialUser {id: 1})-[r:FRIENDS_WITH {id: '1_2'}]->(:SocialUser {id: 2})
                        RETURN properties(r) AS properties
                        """)
                .fetchAs(Map.class)
                .mappedBy((typeSystem, record) -> record.get("properties").asMap())
                .one()
                .orElseThrow();

        assertThat(friendship).containsAllEntriesOf(Map.ofEntries(
                Map.entry("createdAt", CREATED_AT),
                Map.entry("intimacy", 0.5),
                Map.entry("lowFriendAlias", "낮은쪽별명"),
                Map.entry("lowInterestScore", 30.0),
                Map.entry("lowLastInteractedAt", LOW_INTERACTED_AT),
                Map.entry("lowIsMuted", false),
                Map.entry("lowIsRoutable", true),
                Map.entry("highFriendAlias", "높은쪽별명"),
                Map.entry("highInterestScore", 70.0),
                Map.entry("highLastInteractedAt", HIGH_INTERACTED_AT),
                Map.entry("highIsMuted", true),
                Map.entry("highIsRoutable", false)
        ));
    }

    @Test
    @DisplayName("비활성 사용자의 친구 관계도 이관된다")
    void apply_includesInactiveUser() {
        // when
        migrations.apply();

        // then
        assertThat(count("MATCH (:SocialUser {id: 1})-[r:FRIENDS_WITH {id: '1_3'}]->(:InactiveSocialUser {id: 3}) RETURN count(r)"))
                .isEqualTo(1L);
    }

    @Test
    @DisplayName("이관 뒤에는 Friendship 노드와 HAS_FRIENDSHIP 관계가 남지 않는다")
    void apply_removesHexStructure() {
        // when
        migrations.apply();

        // then
        assertThat(count("MATCH (f:Friendship) RETURN count(f)")).isZero();
        assertThat(count("MATCH ()-[r:HAS_FRIENDSHIP]->() RETURN count(r)")).isZero();
        assertThat(count("MATCH ()-[r:FRIENDS_WITH]->() RETURN count(r)")).isEqualTo(2L);
    }

    @Test
    @DisplayName("다시 실행해도 데이터가 바뀌지 않는다")
    void apply_twice_keepsData() {
        // given
        migrations.apply();
        neo4jClient.query("MATCH ()-[r:FRIENDS_WITH {id: '1_2'}]->() SET r.lowInterestScore = 99.0").run();

        // when
        migrations.apply();

        // then
        assertThat(count("MATCH ()-[r:FRIENDS_WITH]->() RETURN count(r)")).isEqualTo(2L);
        assertThat(count("MATCH ()-[r:FRIENDS_WITH {id: '1_2', lowInterestScore: 99.0}]->() RETURN count(r)"))
                .isEqualTo(1L);
    }

    @Test
    @DisplayName("이관 뒤에는 같은 id의 FRIENDS_WITH 관계를 만들 수 없다")
    void apply_rejectsDuplicateFriendshipId() {
        // given
        migrations.apply();

        // when & then
        assertThatThrownBy(() -> neo4jClient.query("""
                        MATCH (a:SocialUser {id: 1}), (b:SocialUser {id: 2})
                        CREATE (a)-[:FRIENDS_WITH {id: '1_2'}]->(b)
                        """).run())
                .hasMessageContaining("already exists");
    }

    private Long count(String cypher) {
        return neo4jClient.query(cypher).fetchAs(Long.class).one().orElseThrow();
    }

    private void clean() {
        neo4jClient.query("DROP CONSTRAINT friends_with_id_unique IF EXISTS").run();
        neo4jClient.query("MATCH (n) DETACH DELETE n").run();
    }
}
