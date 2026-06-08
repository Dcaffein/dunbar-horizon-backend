package com.example.DunbarHorizon.social.adapter.out;

import com.example.DunbarHorizon.social.adapter.out.persistence.neo4j.SocialNetworkRepositoryAdapter;
import com.example.DunbarHorizon.social.application.dto.result.NetworkFriendEdgeResult;
import com.example.DunbarHorizon.social.application.port.out.SocialNetworkRepository;
import com.example.DunbarHorizon.social.domain.friend.DunbarCircle;
import com.example.DunbarHorizon.support.Neo4jRepositoryTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.neo4j.core.Neo4jClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @Cacheable AOP 동작 검증 — 실제 Redis 없이 ConcurrentMapCacheManager로 캐시 레이어를 테스트한다.
 *
 * 핵심 시나리오:
 *   1. 첫 번째 조회 → Neo4j 실행 후 결과를 캐시에 저장
 *   2. Neo4j 데이터 전체 삭제
 *   3. 두 번째 조회 → 캐시 히트: Neo4j를 거치지 않고 이전 결과 반환
 *   4. 캐시 무효화 후 재조회 → Neo4j 실행: 빈 결과 반환
 */
@Neo4jRepositoryTest
@Import({SocialNetworkRepositoryAdapter.class, SocialNetworkRepositoryAdapterCacheTest.InMemoryCacheConfig.class})
class SocialNetworkRepositoryAdapterCacheTest {

    @TestConfiguration
    @EnableCaching
    static class InMemoryCacheConfig {
        @Bean
        public CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("dunbar:network:default", "dunbar:network:label");
        }
    }

    @Autowired
    private SocialNetworkRepository repository;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private Neo4jClient neo4jClient;

    @BeforeEach
    void setUp() {
        // 캐시 초기화
        cacheManager.getCacheNames().forEach(name ->
                cacheManager.getCache(name).clear()
        );

        // me(1) ↔ A(10) ↔ B(20), A-B 간 내부 엣지 존재 → getDefaultIntimacyNetwork가 비어있지 않은 결과 반환
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
    @DisplayName("동일 파라미터로 두 번 조회 시 두 번째는 캐시에서 반환한다 (Neo4j 데이터 삭제 후에도 캐시 결과 유지)")
    void 동일_파라미터_두번째_조회는_캐시에서_반환된다() {
        // given — 첫 번째 조회: Neo4j에서 A-B 엣지 가져옴
        List<NetworkFriendEdgeResult> first = repository.getDefaultIntimacyNetwork(1L, DunbarCircle.DUNBAR);
        assertThat(first).isNotEmpty();

        // Neo4j 데이터 전부 삭제 (캐시는 그대로)
        neo4jClient.query("MATCH (n) DETACH DELETE n").run();

        // when — 두 번째 조회: 캐시 히트 → 삭제된 Neo4j 대신 캐시 결과 반환
        List<NetworkFriendEdgeResult> second = repository.getDefaultIntimacyNetwork(1L, DunbarCircle.DUNBAR);

        // then
        assertThat(second).isNotEmpty();
        assertThat(second).isEqualTo(first);
    }

    @Test
    @DisplayName("캐시 무효화 후 재조회 시 Neo4j를 다시 조회한다")
    void 캐시_무효화_후_재조회하면_Neo4j를_다시_조회한다() {
        // given — 첫 번째 조회로 캐시 채움
        repository.getDefaultIntimacyNetwork(1L, DunbarCircle.DUNBAR);

        // Neo4j 데이터 삭제 후 캐시 무효화
        neo4jClient.query("MATCH (n) DETACH DELETE n").run();
        cacheManager.getCache("dunbar:network:default").evict("1:DUNBAR");

        // when — 캐시 미스 → Neo4j 재조회 (데이터 없음)
        List<NetworkFriendEdgeResult> result = repository.getDefaultIntimacyNetwork(1L, DunbarCircle.DUNBAR);

        // then — Neo4j가 비어있으므로 빈 결과 반환
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("DunbarCircle이 다르면 별도의 캐시 키를 사용한다")
    void 다른_circleSize는_별도_캐시_키를_가진다() {
        // when
        repository.getDefaultIntimacyNetwork(1L, DunbarCircle.DUNBAR);
        repository.getDefaultIntimacyNetwork(1L, DunbarCircle.SUPPORT);

        // then — 각각 별도 키로 캐시에 저장됨
        assertThat(cacheManager.getCache("dunbar:network:default").get("1:DUNBAR")).isNotNull();
        assertThat(cacheManager.getCache("dunbar:network:default").get("1:SUPPORT")).isNotNull();
    }

    @Test
    @DisplayName("userId가 다르면 별도의 캐시 키를 사용한다")
    void 다른_userId는_별도_캐시_키를_가진다() {
        // given — userId=1 캐시 채움
        repository.getDefaultIntimacyNetwork(1L, DunbarCircle.DUNBAR);

        // then — userId=2 캐시 키는 없음
        assertThat(cacheManager.getCache("dunbar:network:default").get("2:DUNBAR")).isNull();
        assertThat(cacheManager.getCache("dunbar:network:default").get("1:DUNBAR")).isNotNull();
    }

    @Test
    @DisplayName("라벨 네트워크도 첫 번째 조회 이후 캐시에 저장된다")
    void 라벨_네트워크도_캐시에_저장된다() {
        // given — 라벨 데이터 추가
        neo4jClient.query("""
                MATCH (me:UserReference {id: 1}), (fa:UserReference {id: 10}), (fb:UserReference {id: 20})
                CREATE (lbl:Label {id: 'label-cache-test'})
                CREATE (me)-[:HAS_LABEL]->(lbl)
                CREATE (lbl)-[:HAS_MEMBER]->(fa)
                CREATE (lbl)-[:HAS_MEMBER]->(fb)
                """).run();

        // when
        repository.getLabelCustomNetwork(1L, "label-cache-test");

        // then
        assertThat(cacheManager.getCache("dunbar:network:label").get("1:label-cache-test")).isNotNull();
    }
}
