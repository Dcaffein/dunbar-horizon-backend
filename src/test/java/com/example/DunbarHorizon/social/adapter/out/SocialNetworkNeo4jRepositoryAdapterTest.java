package com.example.DunbarHorizon.social.adapter.out;

import com.example.DunbarHorizon.social.application.dto.result.NetworkFriendEdgeResult;
import com.example.DunbarHorizon.social.application.dto.result.NetworkOneHopsByTwoHopResult;
import com.example.DunbarHorizon.support.Neo4jRepositoryTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.neo4j.core.Neo4jClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Neo4jRepositoryTest
@Import(SocialNetworkNeo4jRepositoryAdapter.class)
class SocialNetworkNeo4jRepositoryAdapterTest {

    @Autowired
    private SocialNetworkNeo4jRepositoryAdapter socialNetworkRepository;

    @Autowired
    private Neo4jClient neo4jClient;

    @BeforeEach
    void setupGraph() {
        neo4jClient.query("MATCH (n) DETACH DELETE n").run();

        /*
         * 데이터 모델 규칙 준수: (User)-[:HAS_FRIENDSHIP]->(Friendship)<-[:HAS_FRIENDSHIP]-(User)
         * 프라이버시 정책: isRoutable은 타겟 User에서 출발하는 :HAS_FRIENDSHIP 엣지의 속성으로 부여
         */
        neo4jClient.query("""
            CREATE (me:UserReference {id: 1, nickname: '나'})
            CREATE (fa:UserReference {id: 10, nickname: '친구A'})
            CREATE (fb:UserReference {id: 20, nickname: '친구B'})
            CREATE (fc:UserReference {id: 30, nickname: '친구C'})
            CREATE (tx:UserReference {id: 100, nickname: '타겟X'})
            
            // 1촌 관계: (나) - (A, B, C)
            CREATE (me)-[:HAS_FRIENDSHIP {isRoutable: true}]->(:Friendship {intimacy: 0.9})<-[:HAS_FRIENDSHIP {isRoutable: true}]-(fa)
            CREATE (me)-[:HAS_FRIENDSHIP {isRoutable: true}]->(:Friendship {intimacy: 0.5})<-[:HAS_FRIENDSHIP {isRoutable: true}]-(fb)
            CREATE (me)-[:HAS_FRIENDSHIP {isRoutable: true}]->(:Friendship {intimacy: 0.1})<-[:HAS_FRIENDSHIP {isRoutable: true}]-(fc)
            
            // 친구들 간의 관계: A와 B는 서로 친구임 (Edge 탐색용)
            CREATE (fa)-[:HAS_FRIENDSHIP {isRoutable: true}]->(:Friendship {intimacy: 0.8})<-[:HAS_FRIENDSHIP {isRoutable: true}]-(fb)
            
            // 2-Hop 관계 및 프라이버시 관문 (Intersection 탐색용)
            // 타겟X가 친구B를 통한 노출은 허용함 (isRoutable: true)
            CREATE (tx)-[:HAS_FRIENDSHIP {isRoutable: true}]->(:Friendship {intimacy: 0.6})<-[:HAS_FRIENDSHIP {isRoutable: true}]-(fb)
            
            // 타겟X가 친구C를 통한 노출은 차단함 (isRoutable: false)
            CREATE (tx)-[:HAS_FRIENDSHIP {isRoutable: false}]->(:Friendship {intimacy: 0.4})<-[:HAS_FRIENDSHIP {isRoutable: true}]-(fc)
        """).run();
    }

    @Test
    @DisplayName("내 친구들 사이의 모든 유효한 친구 관계(Edge)를 정확히 추출한다")
    void getFriendsNetwork_Success() {
        List<NetworkFriendEdgeResult> result = socialNetworkRepository.getFriendsNetwork(1L);

        // A-B 사이의 엣지 1개만 반환되어야 함
        assertThat(result).hasSize(1);
        assertThat(result.get(0).friendAId()).isIn(10L, 20L);
        assertThat(result.get(0).friendBId()).isIn(10L, 20L);
        assertThat(result.get(0).intimacy()).isEqualTo(0.8);
    }

    @Test
    @DisplayName("입력된 특정 타겟들 내에서만 교차 검증하여 친구 관계(Edge)를 추출한다")
    void getVerifiedFriendsNetwork_Success() {
        // 친구A(10)와 친구C(30)만 넘김 (이 둘은 친구가 아님)
        List<NetworkFriendEdgeResult> result = socialNetworkRepository.getVerifiedFriendsNetwork(1L, List.of(10L, 30L));

        assertThat(result).isEmpty();

        // 친구A(10)와 친구B(20)를 넘기면 엣지가 나옴
        List<NetworkFriendEdgeResult> validResult = socialNetworkRepository.getVerifiedFriendsNetwork(1L, List.of(10L, 20L));
        assertThat(validResult).hasSize(1);
    }

    @Test
    @DisplayName("가장 친밀도가 높은 상위 Core 친구를 기점으로 Boundary 내의 네트워크를 조회한다")
    void getTopIntimateFriendsNetwork_Success() {
        // Boundary를 3명(A,B,C)으로 잡고, 그중 상위 1명(A)을 기점으로 탐색
        // A와 B의 관계가 반환되어야 함
        List<NetworkFriendEdgeResult> result = socialNetworkRepository.getTopIntimateFriendsNetwork(1L, 3, 1);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).friendAId()).isIn(10L, 20L);
        assertThat(result.get(0).friendBId()).isIn(10L, 20L);
    }

    @Test
    @DisplayName("2-Hop 타겟과의 공통 친구 조회 시 타겟이 엣지에 설정한 프라이버시(isRoutable)를 준수한다")
    void getIntersectionOneHops_PrivacyFilterTest() {
        // 나와 타겟X(100) 사이의 물리적인 공통 친구는 B(20), C(30) 두 명임
        // 하지만 C 쪽의 엣지에는 isRoutable: false가 걸려 있음
        List<NetworkOneHopsByTwoHopResult> result = socialNetworkRepository.getIntersectionOneHops(1L, 100L);

        // 결과적으로 노출이 허용된 B(20L)만 반환되어야 함
        assertThat(result).hasSize(1);
        assertThat(result.get(0).friendId()).isEqualTo(20L);
    }
}