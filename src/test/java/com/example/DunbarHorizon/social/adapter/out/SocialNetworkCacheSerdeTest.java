package com.example.DunbarHorizon.social.adapter.out;

import com.example.DunbarHorizon.social.application.dto.result.NetworkGraphResult;
import com.example.DunbarHorizon.social.application.dto.result.NodeEdgeResult;
import com.example.DunbarHorizon.social.application.dto.result.NodeGraphResult;
import com.example.DunbarHorizon.social.application.port.out.SocialNetworkRepository;
import com.example.DunbarHorizon.social.domain.friend.DunbarCircle;
import com.example.DunbarHorizon.support.TestContainerConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Redis 역직렬화 통합 테스트.
 *
 * NetworkGraphResult는 record(final class)이므로 GenericJackson2JsonRedisSerializer의
 * NON_FINAL 정책에서 @class 타입 정보가 생략 → LinkedHashMap으로 역직렬화되어
 * ClassCastException이 발생한다. RedisConfig에서 해당 캐시에 Jackson2JsonRedisSerializer를
 * 명시함으로써 이 문제를 해결한 것을 검증한다.
 *
 * @DataNeo4jTest 슬라이스는 @EnableCaching을 로드하지 않아 @Cacheable이 우회되므로
 * @SpringBootTest + 실제 Redis 컨테이너가 반드시 필요하다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Import(TestContainerConfig.class)
class SocialNetworkCacheSerdeTest {

    private static final GenericContainer<?> REDIS =
            new GenericContainer<>("redis:7.0")
                    .withExposedPorts(6379)
                    .withReuse(true);

    static {
        REDIS.start();
    }

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    // SecurityConfig.filterChain()이 oauth2Login()에서 ClientRegistrationRepository를 찾으므로
    // 테스트 컨텍스트에 mock으로 등록해 OAuth2 자동 설정 실패를 방지한다
    @MockBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @Autowired
    private SocialNetworkRepository networkRepository;

    @Autowired
    private Neo4jClient neo4jClient;

    @Autowired
    private CacheManager cacheManager;

    /**
     * 테스트 그래프 (SocialNetworkRepositoryAdapterTest와 동일):
     *   me(1) ↔ A(10)[0.9], B(20)[0.8], C(30)[0.7], D(40)[0.6], E(50)[0.5], F(60)[0.1]
     *   A(10) ↔ B(20)[0.85]
     *   A(10) ↔ F(60)[0.4]
     *   label(test-label-id): A(10), B(20) 포함
     */
    @BeforeEach
    void setUp() {
        neo4jClient.query("MATCH (n) DETACH DELETE n").run();
        neo4jClient.query("""
                CREATE (me:UserReference {id: 1})
                CREATE (fa:UserReference {id: 10})
                CREATE (fb:UserReference {id: 20})
                CREATE (fc:UserReference {id: 30})
                CREATE (fd:UserReference {id: 40})
                CREATE (fe:UserReference {id: 50})
                CREATE (ff:UserReference {id: 60})

                CREATE (me)-[:HAS_FRIENDSHIP {isRoutable: true, interestScore: 0.7}]->(:Friendship {intimacy: 0.9})<-[:HAS_FRIENDSHIP {isRoutable: true, interestScore: 0.0}]-(fa)
                CREATE (me)-[:HAS_FRIENDSHIP {isRoutable: true, interestScore: 0.3}]->(:Friendship {intimacy: 0.8})<-[:HAS_FRIENDSHIP {isRoutable: true, interestScore: 0.0}]-(fb)
                CREATE (me)-[:HAS_FRIENDSHIP {isRoutable: true, interestScore: 0.0}]->(:Friendship {intimacy: 0.7})<-[:HAS_FRIENDSHIP {isRoutable: true, interestScore: 0.0}]-(fc)
                CREATE (me)-[:HAS_FRIENDSHIP {isRoutable: true, interestScore: 0.0}]->(:Friendship {intimacy: 0.6})<-[:HAS_FRIENDSHIP {isRoutable: true, interestScore: 0.0}]-(fd)
                CREATE (me)-[:HAS_FRIENDSHIP {isRoutable: true, interestScore: 0.0}]->(:Friendship {intimacy: 0.5})<-[:HAS_FRIENDSHIP {isRoutable: true, interestScore: 0.0}]-(fe)
                CREATE (me)-[:HAS_FRIENDSHIP {isRoutable: true, interestScore: 0.0}]->(:Friendship {intimacy: 0.1})<-[:HAS_FRIENDSHIP {isRoutable: true, interestScore: 0.0}]-(ff)

                CREATE (fa)-[:HAS_FRIENDSHIP {isRoutable: true, interestScore: 0.0}]->(:Friendship {intimacy: 0.85})<-[:HAS_FRIENDSHIP {isRoutable: true, interestScore: 0.0}]-(fb)
                CREATE (fa)-[:HAS_FRIENDSHIP {isRoutable: true, interestScore: 0.0}]->(:Friendship {intimacy: 0.4})<-[:HAS_FRIENDSHIP {isRoutable: true, interestScore: 0.0}]-(ff)

                CREATE (lbl:Label {id: 'test-label-id'})
                CREATE (me)-[:HAS_LABEL]->(lbl)
                CREATE (lbl)-[:HAS_MEMBER]->(fa)
                CREATE (lbl)-[:HAS_MEMBER]->(fb)
                """).run();

        cacheManager.getCache("dunbar:network:default").clear();
        cacheManager.getCache("dunbar:network:label").clear();
    }

    @AfterEach
    void tearDown() {
        cacheManager.getCache("dunbar:network:default").clear();
        cacheManager.getCache("dunbar:network:label").clear();
    }

    // ── getDefaultNetworkGraph ────────────────────────────────────────────────

    @Test
    @DisplayName("첫 번째 호출 후 Redis 캐시에 NetworkGraphResult 타입으로 저장된다")
    void getDefaultNetworkGraph_첫_호출_후_Redis에_NetworkGraphResult로_저장된다() {
        networkRepository.getDefaultNetworkGraph(1L, DunbarCircle.DUNBAR);

        Cache.ValueWrapper cached = cacheManager.getCache("dunbar:network:default").get("1:DUNBAR");
        assertThat(cached).isNotNull();
        assertThat(cached.get()).isInstanceOf(NetworkGraphResult.class);
    }

    @Test
    @DisplayName("두 번째 호출 시 Redis에서 역직렬화해도 ClassCastException 없이 NetworkGraphResult를 반환한다")
    void getDefaultNetworkGraph_두번째_호출은_Redis_역직렬화가_정상_동작한다() {
        // 첫 번째 호출: Neo4j → Redis 저장
        NetworkGraphResult first = networkRepository.getDefaultNetworkGraph(1L, DunbarCircle.DUNBAR);

        // 두 번째 호출: Redis → 역직렬화 (ClassCastException이 발생하지 않아야 함)
        assertThatNoException().isThrownBy(() -> {
            NetworkGraphResult second = networkRepository.getDefaultNetworkGraph(1L, DunbarCircle.DUNBAR);
            assertThat(second).isEqualTo(first);
        });
    }

    @Test
    @DisplayName("Redis에서 복원된 NetworkGraphResult는 6개 노드와 총 4개 엣지를 정확히 보존한다")
    void getDefaultNetworkGraph_역직렬화된_중첩_구조가_정확히_복원된다() {
        // 첫 번째 호출로 캐시 적재
        networkRepository.getDefaultNetworkGraph(1L, DunbarCircle.DUNBAR);

        // 두 번째 호출은 Redis에서 반환
        NetworkGraphResult fromCache = networkRepository.getDefaultNetworkGraph(1L, DunbarCircle.DUNBAR);

        assertThat(fromCache.nodes()).hasSize(6);
        assertThat(fromCache.nodes()).allSatisfy(node -> {
            assertThat(node).isInstanceOf(NodeGraphResult.class);
            assertThat(node.nodeId()).isNotNull();
            node.edges().forEach(edge -> assertThat(edge).isInstanceOf(NodeEdgeResult.class));
        });

        // A(10)-B(20), A(10)-F(60) 양방향 총 4개 엣지
        long totalEdges = fromCache.nodes().stream().mapToLong(n -> n.edges().size()).sum();
        assertThat(totalEdges).isEqualTo(4);
    }

    @Test
    @DisplayName("Redis에서 복원된 노드의 interestScore 수치 정밀도가 보존된다")
    void getDefaultNetworkGraph_역직렬화된_interestScore_수치가_보존된다() {
        networkRepository.getDefaultNetworkGraph(1L, DunbarCircle.DUNBAR);

        NetworkGraphResult fromCache = networkRepository.getDefaultNetworkGraph(1L, DunbarCircle.DUNBAR);

        NodeGraphResult nodeA = fromCache.nodes().stream()
                .filter(n -> n.nodeId().equals(10L))
                .findFirst()
                .orElseThrow();
        assertThat(nodeA.interestScore()).isEqualTo(0.7);
    }

    // ── getLabelCustomNetwork ─────────────────────────────────────────────────

    @Test
    @DisplayName("두 번째 호출 시 Redis에서 역직렬화해도 ClassCastException 없이 NetworkGraphResult를 반환한다")
    void getLabelCustomNetwork_두번째_호출은_Redis_역직렬화가_정상_동작한다() {
        NetworkGraphResult first = networkRepository.getLabelCustomNetwork(1L, "test-label-id");

        assertThatNoException().isThrownBy(() -> {
            NetworkGraphResult second = networkRepository.getLabelCustomNetwork(1L, "test-label-id");
            assertThat(second).isEqualTo(first);
        });
    }

    @Test
    @DisplayName("Redis에서 복원된 라벨 네트워크는 2개 노드와 총 2개 엣지를 정확히 보존한다")
    void getLabelCustomNetwork_역직렬화된_중첩_구조가_정확히_복원된다() {
        networkRepository.getLabelCustomNetwork(1L, "test-label-id");

        NetworkGraphResult fromCache = networkRepository.getLabelCustomNetwork(1L, "test-label-id");

        // label에는 A(10), B(20)만 포함
        assertThat(fromCache.nodes()).hasSize(2);
        assertThat(fromCache.nodes()).extracting(NodeGraphResult::nodeId)
                .containsExactlyInAnyOrder(10L, 20L);

        // A-B 양방향 엣지
        long totalEdges = fromCache.nodes().stream().mapToLong(n -> n.edges().size()).sum();
        assertThat(totalEdges).isEqualTo(2);
    }
}
