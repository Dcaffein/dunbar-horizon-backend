package com.example.DunbarHorizon.social.adapter.out;

import com.example.DunbarHorizon.social.adapter.out.persistence.neo4j.SocialNetworkRepositoryAdapter;
import com.example.DunbarHorizon.social.application.dto.result.NetworkGraphResult;
import com.example.DunbarHorizon.social.application.port.out.SocialNetworkRepository;
import com.example.DunbarHorizon.social.domain.friend.DunbarCircle;
import com.example.DunbarHorizon.support.Neo4jRepositoryTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.neo4j.core.Neo4jClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @Cacheable은 서비스 레이어(SocialNetworkQueryService)로 이동됨.
 * 이 파일은 adapter의 getDefaultNetworkGraph 기본 동작만 검증한다.
 */
@Neo4jRepositoryTest
@Import(SocialNetworkRepositoryAdapter.class)
class SocialNetworkRepositoryAdapterCacheTest {

    @Autowired
    private SocialNetworkRepository repository;

    @Autowired
    private Neo4jClient neo4jClient;

    @BeforeEach
    void setUp() {
        neo4jClient.query("MATCH (n) DETACH DELETE n").run();
        neo4jClient.query("""
                CREATE (me:UserReference {id: 1})
                CREATE (fa:UserReference {id: 10})
                CREATE (fb:UserReference {id: 20})
                CREATE (me)-[:HAS_FRIENDSHIP {isRoutable: true, interestScore: 0.0}]->(:Friendship {intimacy: 0.9})<-[:HAS_FRIENDSHIP {isRoutable: true, interestScore: 0.0}]-(fa)
                CREATE (me)-[:HAS_FRIENDSHIP {isRoutable: true, interestScore: 0.0}]->(:Friendship {intimacy: 0.8})<-[:HAS_FRIENDSHIP {isRoutable: true, interestScore: 0.0}]-(fb)
                CREATE (fa)-[:HAS_FRIENDSHIP {isRoutable: true, interestScore: 0.0}]->(:Friendship {intimacy: 0.75})<-[:HAS_FRIENDSHIP {isRoutable: true, interestScore: 0.0}]-(fb)
                """).run();
    }

    @Test
    @DisplayName("getDefaultNetworkGraph: 노드와 엣지를 함께 반환한다")
    void getDefaultNetworkGraph_노드와_엣지를_함께_반환한다() {
        NetworkGraphResult result = repository.getDefaultNetworkGraph(1L, DunbarCircle.DUNBAR, 5, 10);

        assertThat(result.nodes()).hasSize(2); // A, B
        long totalEdges = result.nodes().stream().mapToLong(n -> n.edges().size()).sum();
        assertThat(totalEdges).isEqualTo(2); // A→B, B→A
    }

    @Test
    @DisplayName("getDefaultNetworkGraph: 데이터가 없으면 빈 nodes를 반환한다")
    void getDefaultNetworkGraph_데이터가_없으면_빈_nodes를_반환한다() {
        neo4jClient.query("MATCH (n) DETACH DELETE n").run();

        NetworkGraphResult result = repository.getDefaultNetworkGraph(1L, DunbarCircle.DUNBAR, 5, 10);

        assertThat(result.nodes()).isEmpty();
    }

    @Test
    @DisplayName("getDefaultNetworkGraph: DunbarCircle SUPPORT는 상위 친구만 반환한다")
    void getDefaultNetworkGraph_SUPPORT_상위_친구만_반환한다() {
        // me(1)의 친구는 A(10, intimacy=0.9), B(20, intimacy=0.8) 2명 → SUPPORT(5) 이내
        NetworkGraphResult result = repository.getDefaultNetworkGraph(1L, DunbarCircle.SUPPORT, 5, 10);

        assertThat(result.nodes()).hasSize(2);
    }
}
