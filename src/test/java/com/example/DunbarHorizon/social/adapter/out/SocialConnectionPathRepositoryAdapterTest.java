package com.example.DunbarHorizon.social.adapter.out;

import com.example.DunbarHorizon.social.adapter.out.persistence.neo4j.SocialConnectionPathRepositoryAdapter;
import com.example.DunbarHorizon.social.application.dto.result.ConnectionPathResult;
import com.example.DunbarHorizon.support.Neo4jRepositoryTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.neo4j.core.Neo4jClient;

import static org.assertj.core.api.Assertions.assertThat;

@Neo4jRepositoryTest
@Import(SocialConnectionPathRepositoryAdapter.class)
class SocialConnectionPathRepositoryAdapterTest {

    private static final int LIMIT = 3;

    @Autowired
    private SocialConnectionPathRepositoryAdapter connectionPathRepository;

    @Autowired
    private Neo4jClient neo4jClient;

    @BeforeEach
    void setupGraph() {
        neo4jClient.query("MATCH (n) DETACH DELETE n").run();

        /*
         * 나(1)와 세 타겟
         * - 타겟99: 중개인 5명(2~6, score 0.9→0.5) + 노출 차단 2명(7, 8)
         *   7은 타겟 쪽 차단, 8은 중개인 쪽 차단. 둘 다 intimacy 1.0이라
         *   필터가 없으면 상위 두 자리를 차지한다
         * - 타겟98: 중개인 2명(2, 3) — 상한 이하 케이스
         * - 타겟97: 중개인 없음
         */
        neo4jClient.query("""
            CREATE (me:UserReference {id: 1, nickname: '나'})
            CREATE (t99:UserReference {id: 99, nickname: '타겟99'})
            CREATE (t98:UserReference {id: 98, nickname: '타겟98'})
            CREATE (t97:UserReference {id: 97, nickname: '타겟97'})

            CREATE (m2:UserReference {id: 2, nickname: '중개인2'})
            CREATE (m3:UserReference {id: 3, nickname: '중개인3'})
            CREATE (m4:UserReference {id: 4, nickname: '중개인4'})
            CREATE (m5:UserReference {id: 5, nickname: '중개인5'})
            CREATE (m6:UserReference {id: 6, nickname: '중개인6'})
            CREATE (m7:UserReference {id: 7, nickname: '타겟차단'})
            CREATE (m8:UserReference {id: 8, nickname: '중개인차단'})

            // 나와 중개인들의 친구 관계
            CREATE (me)-[:FRIENDS_WITH {intimacy: 0.9, lowIsRoutable: true, highIsRoutable: true}]->(m2)
            CREATE (me)-[:FRIENDS_WITH {intimacy: 0.8, lowIsRoutable: true, highIsRoutable: true}]->(m3)
            CREATE (me)-[:FRIENDS_WITH {intimacy: 0.7, lowIsRoutable: true, highIsRoutable: true}]->(m4)
            CREATE (me)-[:FRIENDS_WITH {intimacy: 0.6, lowIsRoutable: true, highIsRoutable: true}]->(m5)
            CREATE (me)-[:FRIENDS_WITH {intimacy: 0.5, lowIsRoutable: true, highIsRoutable: true}]->(m6)
            CREATE (me)-[:FRIENDS_WITH {intimacy: 1.0, lowIsRoutable: true, highIsRoutable: true}]->(m7)
            CREATE (me)-[:FRIENDS_WITH {intimacy: 1.0, lowIsRoutable: true, highIsRoutable: false}]->(m8)

            // 중개인들과 타겟99의 친구 관계
            CREATE (m2)-[:FRIENDS_WITH {intimacy: 0.9, lowIsRoutable: true, highIsRoutable: true}]->(t99)
            CREATE (m3)-[:FRIENDS_WITH {intimacy: 0.8, lowIsRoutable: true, highIsRoutable: true}]->(t99)
            CREATE (m4)-[:FRIENDS_WITH {intimacy: 0.7, lowIsRoutable: true, highIsRoutable: true}]->(t99)
            CREATE (m5)-[:FRIENDS_WITH {intimacy: 0.6, lowIsRoutable: true, highIsRoutable: true}]->(t99)
            CREATE (m6)-[:FRIENDS_WITH {intimacy: 0.5, lowIsRoutable: true, highIsRoutable: true}]->(t99)
            CREATE (m7)-[:FRIENDS_WITH {intimacy: 1.0, lowIsRoutable: true, highIsRoutable: false}]->(t99)
            CREATE (m8)-[:FRIENDS_WITH {intimacy: 1.0, lowIsRoutable: true, highIsRoutable: true}]->(t99)

            // 중개인 2명뿐인 타겟98
            CREATE (m2)-[:FRIENDS_WITH {intimacy: 0.4, lowIsRoutable: true, highIsRoutable: true}]->(t98)
            CREATE (m3)-[:FRIENDS_WITH {intimacy: 0.3, lowIsRoutable: true, highIsRoutable: true}]->(t98)
        """).run();
    }

    @Test
    @DisplayName("중개인과 타겟이 낮은 id 쪽이어도 그 사용자의 isRoutable로 거른다")
    void findIntermediaries_낮은_id_쪽_차단() {
        // given: 나(1000)가 가장 높은 id라 중개인이 관계의 시작 노드, 타겟(500)이 중개인보다 낮은 id
        neo4jClient.query("""
            CREATE (me:UserReference {id: 1000, nickname: '높은나'})
            CREATE (target:UserReference {id: 500, nickname: '낮은타겟'})
            CREATE (open:UserReference {id: 600, nickname: '공개중개인'})
            CREATE (closed:UserReference {id: 700, nickname: '차단중개인'})
            CREATE (open)-[:FRIENDS_WITH {intimacy: 0.9, lowIsRoutable: true, highIsRoutable: false}]->(me)
            CREATE (closed)-[:FRIENDS_WITH {intimacy: 0.9, lowIsRoutable: false, highIsRoutable: true}]->(me)
            CREATE (target)-[:FRIENDS_WITH {intimacy: 0.9, lowIsRoutable: true, highIsRoutable: false}]->(open)
            CREATE (target)-[:FRIENDS_WITH {intimacy: 0.9, lowIsRoutable: true, highIsRoutable: true}]->(closed)
        """).run();

        // when
        ConnectionPathResult.Intermediaries result = connectionPathRepository.findIntermediaries(1000L, 500L, LIMIT);

        // then
        assertThat(result.items())
                .extracting(ConnectionPathResult.IntermediaryResult::userId)
                .containsExactly(600L);
    }

    @Test
    @DisplayName("공통 친구가 상한보다 많으면 상위 3명만 반환하고 totalCount는 전체 수를 유지한다")
    void findIntermediaries_상한_적용() {
        // given: 타겟99의 노출 가능한 공통 친구는 5명

        // when
        ConnectionPathResult.Intermediaries result = connectionPathRepository.findIntermediaries(1L, 99L, LIMIT);

        // then
        assertThat(result.items()).hasSize(3);
        assertThat(result.totalCount()).isEqualTo(5);
    }

    @Test
    @DisplayName("반환된 3명은 임의의 3명이 아니라 score 내림차순 상위 3명이다")
    void findIntermediaries_정렬_유지() {
        // when
        ConnectionPathResult.Intermediaries result = connectionPathRepository.findIntermediaries(1L, 99L, LIMIT);

        // then: score = sqrt(0.9*0.9), sqrt(0.8*0.8), sqrt(0.7*0.7) 순
        assertThat(result.items())
                .extracting(ConnectionPathResult.IntermediaryResult::userId)
                .containsExactly(2L, 3L, 4L);
    }

    @Test
    @DisplayName("공통 친구가 상한 이하이면 전부 반환하고 totalCount가 그 수와 같다")
    void findIntermediaries_상한_이하() {
        // when
        ConnectionPathResult.Intermediaries result = connectionPathRepository.findIntermediaries(1L, 98L, LIMIT);

        // then
        assertThat(result.items())
                .extracting(ConnectionPathResult.IntermediaryResult::userId)
                .containsExactly(2L, 3L);
        assertThat(result.totalCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("isRoutable = false인 중개인은 어느 쪽 설정이든 제외되고 totalCount에도 잡히지 않는다")
    void findIntermediaries_노출_차단_제외() {
        // given: 7(타겟 쪽 차단)·8(중개인 쪽 차단)은 intimacy 1.0으로 최상위 후보다

        // when
        ConnectionPathResult.Intermediaries result = connectionPathRepository.findIntermediaries(1L, 99L, 10);

        // then
        assertThat(result.items())
                .extracting(ConnectionPathResult.IntermediaryResult::userId)
                .doesNotContain(7L, 8L)
                .containsExactly(2L, 3L, 4L, 5L, 6L);
        assertThat(result.totalCount()).isEqualTo(5);
    }

    @Test
    @DisplayName("공통 친구가 없으면 빈 목록과 totalCount 0을 반환한다")
    void findIntermediaries_공통_친구_없음() {
        // when
        ConnectionPathResult.Intermediaries result = connectionPathRepository.findIntermediaries(1L, 97L, LIMIT);

        // then
        assertThat(result.items()).isEmpty();
        assertThat(result.totalCount()).isZero();
    }

    @Test
    @DisplayName("닉네임이 함께 반환된다")
    void findIntermediaries_닉네임_반환() {
        // when
        ConnectionPathResult.Intermediaries result = connectionPathRepository.findIntermediaries(1L, 98L, LIMIT);

        // then
        assertThat(result.items())
                .extracting(ConnectionPathResult.IntermediaryResult::nickname)
                .containsExactly("중개인2", "중개인3");
    }
}
