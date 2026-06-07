package com.example.DunbarHorizon.social.adapter.out;

import com.example.DunbarHorizon.social.application.dto.result.MutualFriendEdgeResult;
import com.example.DunbarHorizon.social.application.dto.result.NetworkFriendEdgeResult;
import com.example.DunbarHorizon.social.application.dto.result.NetworkOneHopsByTwoHopResult;
import com.example.DunbarHorizon.social.domain.friend.DunbarCircle;
import com.example.DunbarHorizon.support.Neo4jRepositoryTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.neo4j.core.Neo4jClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 테스트 그래프:
 *   me(1) ↔ A(10)[0.9], B(20)[0.8], C(30)[0.7], D(40)[0.6], E(50)[0.5], F(60)[0.1]
 *   A(10) ↔ B(20)[0.85]  — SUPPORT(5) 경계 안: A,B 모두 포함 → 항상 반환
 *   A(10) ↔ F(60)[0.4]   — SUPPORT(5) 경계 밖: F 제외 → SUPPORT에서는 미반환, DUNBAR에서는 반환
 *   targetX(100) ↔ B(20)[isRoutable=true], C(30)[isRoutable=false]
 *   label(test-label-id): me-A, me-B 포함
 */
@Neo4jRepositoryTest
@Import(SocialNetworkRepositoryAdapter.class)
class SocialNetworkRepositoryAdapterTest {

    @Autowired
    private SocialNetworkRepositoryAdapter repository;

    @Autowired
    private Neo4jClient neo4jClient;

    @BeforeEach
    void setupGraph() {
        neo4jClient.query("MATCH (n) DETACH DELETE n").run();

        neo4jClient.query("""
                CREATE (me:UserReference {id: 1})
                CREATE (fa:UserReference {id: 10})
                CREATE (fb:UserReference {id: 20})
                CREATE (fc:UserReference {id: 30})
                CREATE (fd:UserReference {id: 40})
                CREATE (fe:UserReference {id: 50})
                CREATE (ff:UserReference {id: 60})
                CREATE (tx:UserReference {id: 100})

                CREATE (me)-[:HAS_FRIENDSHIP {isRoutable: true, interestScore: 0.7}]->(:Friendship {intimacy: 0.9})<-[:HAS_FRIENDSHIP {isRoutable: true, interestScore: 0.0}]-(fa)
                CREATE (me)-[:HAS_FRIENDSHIP {isRoutable: true, interestScore: 0.3}]->(:Friendship {intimacy: 0.8})<-[:HAS_FRIENDSHIP {isRoutable: true, interestScore: 0.0}]-(fb)
                CREATE (me)-[:HAS_FRIENDSHIP {isRoutable: true, interestScore: 0.0}]->(:Friendship {intimacy: 0.7})<-[:HAS_FRIENDSHIP {isRoutable: true, interestScore: 0.0}]-(fc)
                CREATE (me)-[:HAS_FRIENDSHIP {isRoutable: true, interestScore: 0.0}]->(:Friendship {intimacy: 0.6})<-[:HAS_FRIENDSHIP {isRoutable: true, interestScore: 0.0}]-(fd)
                CREATE (me)-[:HAS_FRIENDSHIP {isRoutable: true, interestScore: 0.0}]->(:Friendship {intimacy: 0.5})<-[:HAS_FRIENDSHIP {isRoutable: true, interestScore: 0.0}]-(fe)
                CREATE (me)-[:HAS_FRIENDSHIP {isRoutable: true, interestScore: 0.0}]->(:Friendship {intimacy: 0.1})<-[:HAS_FRIENDSHIP {isRoutable: true, interestScore: 0.0}]-(ff)

                CREATE (fa)-[:HAS_FRIENDSHIP {isRoutable: true, interestScore: 0.0}]->(:Friendship {intimacy: 0.85})<-[:HAS_FRIENDSHIP {isRoutable: true, interestScore: 0.0}]-(fb)
                CREATE (fa)-[:HAS_FRIENDSHIP {isRoutable: true, interestScore: 0.0}]->(:Friendship {intimacy: 0.4})<-[:HAS_FRIENDSHIP {isRoutable: true, interestScore: 0.0}]-(ff)

                CREATE (tx)-[:HAS_FRIENDSHIP {isRoutable: true,  interestScore: 0.0}]->(:Friendship {intimacy: 0.6})<-[:HAS_FRIENDSHIP {isRoutable: true,  interestScore: 0.0}]-(fb)
                CREATE (tx)-[:HAS_FRIENDSHIP {isRoutable: false, interestScore: 0.0}]->(:Friendship {intimacy: 0.4})<-[:HAS_FRIENDSHIP {isRoutable: true,  interestScore: 0.0}]-(fc)

                CREATE (lbl:Label {id: 'test-label-id'})
                CREATE (me)-[:HAS_LABEL]->(lbl)
                CREATE (lbl)-[:HAS_MEMBER]->(fa)
                CREATE (lbl)-[:HAS_MEMBER]->(fb)
                """).run();
    }

    // ───────── Default Network ─────────

    @Test
    @DisplayName("DUNBAR 크기로 조회 시 경계 내 모든 친구 간 엣지를 반환한다 (A-B, A-F 양방향 = 4개)")
    void getDefaultIntimacyNetwork_DUNBAR_경계_내_모든_엣지를_반환한다() {
        // when
        List<NetworkFriendEdgeResult> result = repository.getDefaultIntimacyNetwork(1L, DunbarCircle.DUNBAR);

        // then — A-B(두 방향) + A-F(두 방향) = 4
        assertThat(result).hasSize(4);
        assertThat(result).anyMatch(e -> e.friendAId().equals(10L) && e.friendBId().equals(20L));
        assertThat(result).anyMatch(e -> e.friendAId().equals(20L) && e.friendBId().equals(10L));
        assertThat(result).anyMatch(e -> e.friendAId().equals(10L) && e.friendBId().equals(60L));
        assertThat(result).anyMatch(e -> e.friendAId().equals(60L) && e.friendBId().equals(10L));
    }

    @Test
    @DisplayName("SUPPORT(5) 크기로 조회 시 6번째 친구 F가 경계에서 제외되어 A-F 엣지는 반환되지 않는다")
    void getDefaultIntimacyNetwork_SUPPORT_경계_밖의_친구가_포함된_엣지는_반환하지_않는다() {
        // when — SUPPORT=5: top5 = A(0.9), B(0.8), C(0.7), D(0.6), E(0.5). F(0.1)는 제외
        List<NetworkFriendEdgeResult> result = repository.getDefaultIntimacyNetwork(1L, DunbarCircle.SUPPORT);

        // then — A-B(두 방향) = 2, F 관련 엣지 없음
        assertThat(result).hasSize(2);
        assertThat(result).noneMatch(e -> e.friendAId().equals(60L) || e.friendBId().equals(60L));
        assertThat(result).allMatch(e ->
                (e.friendAId().equals(10L) && e.friendBId().equals(20L)) ||
                (e.friendAId().equals(20L) && e.friendBId().equals(10L))
        );
    }

    @Test
    @DisplayName("interestScore가 interestMap lookup으로 올바르게 반환된다")
    void getDefaultIntimacyNetwork_interestScore가_interestMap_lookup으로_올바르게_반환된다() {
        // given: me→A interestScore=0.7, me→B interestScore=0.3 (setupGraph에서 설정)
        // A-B 엣지에서 friendA=A(10), friendB=B(20) or friendA=B(20), friendB=A(10)

        List<NetworkFriendEdgeResult> result = repository.getDefaultIntimacyNetwork(1L, DunbarCircle.DUNBAR);

        // then — A(10)가 friendA인 엣지: friendA_Interest=0.7, B(20)이 friendA인 엣지: friendA_Interest=0.3
        assertThat(result).anyMatch(e ->
                e.friendAId().equals(10L) && e.friendBId().equals(20L)
                && e.friendAInterest() == 0.7 && e.friendBInterest() == 0.3
        );
        assertThat(result).anyMatch(e ->
                e.friendAId().equals(20L) && e.friendBId().equals(10L)
                && e.friendAInterest() == 0.3 && e.friendBInterest() == 0.7
        );
    }

    @Test
    @DisplayName("친구가 없는 유저는 빈 결과를 반환한다")
    void getDefaultIntimacyNetwork_친구가_없는_유저는_빈_결과를_반환한다() {
        // when — userId=999는 그래프에 존재하지 않음
        List<NetworkFriendEdgeResult> result = repository.getDefaultIntimacyNetwork(999L, DunbarCircle.DUNBAR);

        // then
        assertThat(result).isEmpty();
    }

    // ───────── Label Network ─────────

    @Test
    @DisplayName("라벨 네트워크 조회 시 라벨 멤버 간의 엣지만 반환한다")
    void getLabelCustomNetwork_라벨_멤버_간의_엣지만_반환한다() {
        // when — label에 A(10)와 B(20)만 포함
        List<NetworkFriendEdgeResult> result = repository.getLabelCustomNetwork(1L, "test-label-id");

        // then — A-B(두 방향) = 2
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(e ->
                (e.friendAId().equals(10L) && e.friendBId().equals(20L)) ||
                (e.friendAId().equals(20L) && e.friendBId().equals(10L))
        );
        // F(60)는 라벨 멤버가 아니므로 결과에 없어야 함
        assertThat(result).noneMatch(e -> e.friendAId().equals(60L) || e.friendBId().equals(60L));
    }

    @Test
    @DisplayName("존재하지 않는 라벨 ID로 조회하면 빈 결과를 반환한다")
    void getLabelCustomNetwork_존재하지_않는_라벨ID는_빈_결과를_반환한다() {
        // when
        List<NetworkFriendEdgeResult> result = repository.getLabelCustomNetwork(1L, "non-existent-label-id");

        // then
        assertThat(result).isEmpty();
    }

    // ───────── Intersection: Two-Hop ─────────

    @Test
    @DisplayName("2-Hop 공통 친구 조회 시 isRoutable=false인 친구는 반환하지 않는다")
    void getNetworkContactsOfTwoHop_isRoutable_false인_공통친구는_반환하지_않는다() {
        // 나(1)와 타겟X(100)의 물리적 공통 친구: B(20, isRoutable=true), C(30, isRoutable=false)
        List<NetworkOneHopsByTwoHopResult> result =
                repository.getNetworkContactsOfTwoHop(1L, 100L, null, DunbarCircle.DUNBAR.getLimitSize());

        // then — isRoutable=true인 B(20)만 반환
        assertThat(result).hasSize(1);
        assertThat(result.get(0).friendId()).isEqualTo(20L);
    }

    // ───────── Intersection: One-Hop ─────────

    @Test
    @DisplayName("1-Hop 친구 추가 시 현재 스켈레톤 내 isRoutable=true 공통 친구 엣지만 반환한다")
    void getNewNodeEdges_스켈레톤_내_공통_친구를_반환한다() {
        // labelName=null, limitSize=DUNBAR: 스켈레톤 = [A, B, C, D, E, F]
        // X-B: isRoutable=true → 반환, X-C: isRoutable=false → 제외
        List<MutualFriendEdgeResult> result =
                repository.getNewNodeEdges(1L, 100L, null, DunbarCircle.DUNBAR.getLimitSize());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).friendAId()).isEqualTo(100L);
        assertThat(result.get(0).friendBId()).isEqualTo(20L);
    }

    @Test
    @DisplayName("1-Hop 친구 추가 시 스켈레톤 경계 밖의 친구는 교집합에서 제외된다")
    void getNewNodeEdges_스켈레톤_밖의_친구는_교집합에서_제외된다() {
        // SUPPORT(5) 스켈레톤 = [A, B, C, D, E]. F(60)은 제외
        // targetX는 B와 연결 → B는 스켈레톤 안 → 반환됨
        // isRoutable=false인 C는 여전히 제외
        List<MutualFriendEdgeResult> result =
                repository.getNewNodeEdges(1L, 100L, null, DunbarCircle.SUPPORT.getLimitSize());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).friendBId()).isEqualTo(20L);
    }
}
