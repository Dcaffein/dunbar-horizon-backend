package com.example.DunbarHorizon.social.adapter.out;

import com.example.DunbarHorizon.social.application.dto.result.MutualFriendEdgeResult;
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
         *
         * 관계도:
         *   나(1) ↔ 친구A(10) [intimacy: 0.9]
         *   나(1) ↔ 친구B(20) [intimacy: 0.5]
         *   나(1) ↔ 친구C(30) [intimacy: 0.1]
         *   A(10) ↔ B(20)    [intimacy: 0.8]  ← 유일한 친구-친구 내부 엣지
         *   타겟X(100) ↔ B(20) [isRoutable: true]
         *   타겟X(100) ↔ C(30) [isRoutable: false] ← 프라이버시 차단
         */
        neo4jClient.query("""
            CREATE (me:UserReference {id: 1, nickname: '나'})
            CREATE (fa:UserReference {id: 10, nickname: '친구A'})
            CREATE (fb:UserReference {id: 20, nickname: '친구B'})
            CREATE (fc:UserReference {id: 30, nickname: '친구C'})
            CREATE (tx:UserReference {id: 100, nickname: '타겟X'})

            // 1촌 관계: (나) - (A, B, C)
            CREATE (me)-[:HAS_FRIENDSHIP {isRoutable: true, interestScore: 0.0}]->(:Friendship {intimacy: 0.9})<-[:HAS_FRIENDSHIP {isRoutable: true, interestScore: 0.0}]-(fa)
            CREATE (me)-[:HAS_FRIENDSHIP {isRoutable: true, interestScore: 0.0}]->(:Friendship {intimacy: 0.5})<-[:HAS_FRIENDSHIP {isRoutable: true, interestScore: 0.0}]-(fb)
            CREATE (me)-[:HAS_FRIENDSHIP {isRoutable: true, interestScore: 0.0}]->(:Friendship {intimacy: 0.1})<-[:HAS_FRIENDSHIP {isRoutable: true, interestScore: 0.0}]-(fc)

            // 친구들 간의 내부 관계: A와 B만 서로 친구
            CREATE (fa)-[:HAS_FRIENDSHIP {isRoutable: true, interestScore: 0.0}]->(:Friendship {intimacy: 0.8})<-[:HAS_FRIENDSHIP {isRoutable: true, interestScore: 0.0}]-(fb)

            // 2-Hop 관계 및 프라이버시 관문
            CREATE (tx)-[:HAS_FRIENDSHIP {isRoutable: true, interestScore: 0.0}]->(:Friendship {intimacy: 0.6})<-[:HAS_FRIENDSHIP {isRoutable: true, interestScore: 0.0}]-(fb)
            CREATE (tx)-[:HAS_FRIENDSHIP {isRoutable: false, interestScore: 0.0}]->(:Friendship {intimacy: 0.4})<-[:HAS_FRIENDSHIP {isRoutable: true, interestScore: 0.0}]-(fc)
        """).run();
    }

    @Test
    @DisplayName("친밀도 기반 네트워크 조회 시, 친구들 사이의 내부 연결 엣지(A-B)를 반환한다")
    void getDefaultIntimacyNetwork_Success() {
        List<NetworkFriendEdgeResult> result = socialNetworkRepository.getDefaultIntimacyNetwork(1L, 150);

        // A(10)-B(20) 사이 엣지가 양 방향으로 반환됨 (member=A 관점, member=B 관점 각각 1개)
        assertThat(result).isNotEmpty();
        assertThat(result).allSatisfy(edge -> {
            assertThat(edge.friendAId()).isIn(10L, 20L);
            assertThat(edge.friendBId()).isIn(10L, 20L);
            assertThat(edge.intimacy()).isEqualTo(0.8);
        });
        // 친구C(30)는 다른 친구와 직접 연결이 없으므로 결과에 등장하지 않아야 함
        assertThat(result).noneMatch(edge -> edge.friendAId().equals(30L) || edge.friendBId().equals(30L));
    }

    @Test
    @DisplayName("limitSize로 경계 인원을 제한하면 해당 범위 내의 엣지만 반환된다")
    void getDefaultIntimacyNetwork_WithSmallLimitSize() {
        // limitSize=1이면 가장 친밀한 친구A(10)만 포함되어 내부 탐색 결과가 없어야 함
        List<NetworkFriendEdgeResult> result = socialNetworkRepository.getDefaultIntimacyNetwork(1L, 1);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("limitSize가 A, B 둘 다 포함할 만큼 크면 A-B 엣지가 반환된다")
    void getDefaultIntimacyNetwork_WithLimitSizeIncludingBoth() {
        // limitSize=2: 친밀도 상위 2명 (A=0.9, B=0.5), C(30)는 제외됨
        List<NetworkFriendEdgeResult> result = socialNetworkRepository.getDefaultIntimacyNetwork(1L, 2);

        assertThat(result).isNotEmpty();
        assertThat(result).noneMatch(edge -> edge.friendAId().equals(30L) || edge.friendBId().equals(30L));
    }

    @Test
    @DisplayName("2-Hop 타겟과의 공통 친구 조회 시 타겟이 설정한 프라이버시(isRoutable)를 준수한다")
    void getIntersectionOneHops_PrivacyFilterTest() {
        // 나(1)와 타겟X(100)의 물리적 공통 친구: B(20, isRoutable=true), C(30, isRoutable=false)
        // labelName=null이면 글로벌 네트워크 기준으로 스켈레톤 재구성
        List<NetworkOneHopsByTwoHopResult> result = socialNetworkRepository.getIntersectionOneHops(1L, 100L, null, 150);

        // isRoutable=true인 B(20)만 반환되어야 함
        assertThat(result).hasSize(1);
        assertThat(result.get(0).friendId()).isEqualTo(20L);
    }

    @Test
    @DisplayName("1-Hop 친구 추가(Drag & Drop) 시 현재 화면의 스켈레톤 내에서 isRoutable=true인 엣지만 반환한다")
    void getIntersectionByOneHop_Success() {
        // labelName=null, limitSize=150: 글로벌 네트워크 기준 스켈레톤 = [A(10), B(20), C(30)]
        // X-B 관계는 isRoutable=true → 반환, X-C 관계는 isRoutable=false → 제외
        List<MutualFriendEdgeResult> result = socialNetworkRepository.getIntersectionByOneHop(1L, 100L, null, 150);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).friendAId()).isEqualTo(100L);
        assertThat(result.get(0).friendBId()).isEqualTo(20L);
    }

    @Test
    @DisplayName("1-Hop 친구 추가 시 스켈레톤 범위 밖의 친구와는 엣지가 반환되지 않는다")
    void getIntersectionByOneHop_NotInSkeleton() {
        // limitSize=1: 스켈레톤은 가장 친밀한 A(10)만 포함
        // 타겟X(100)는 A와 직접 연결이 없으므로 교집합이 없어야 함
        List<MutualFriendEdgeResult> result = socialNetworkRepository.getIntersectionByOneHop(1L, 100L, null, 1);

        assertThat(result).isEmpty();
    }
}
