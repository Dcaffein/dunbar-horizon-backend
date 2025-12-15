package com.example.GooRoomBe.social.trace.infrastructure;

import com.example.GooRoomBe.social.trace.domain.Trace;
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

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static com.example.GooRoomBe.social.common.SocialSchemaConstants.SOCIAL_USER;
import static org.assertj.core.api.Assertions.assertThat;

@DataNeo4jTest
@Testcontainers
@ActiveProfiles("test")
class TraceRepositoryTest {

    @Container
    @ServiceConnection
    static Neo4jContainer<?> neo4jContainer = new Neo4jContainer<>("neo4j:5");

    @Autowired
    private TraceRepository traceRepository;

    @Autowired
    private Neo4jClient neo4jClient;

    private final String visitorId = "visitor-uuid";
    private final String targetId = "target-uuid";

    @BeforeEach
    void setUp() {
        traceRepository.deleteAll();
        neo4jClient.query("MATCH (n) DETACH DELETE n").run();

        // 유저 노드는 있어야 엣지를 연결할 수 있으므로 미리 생성
        String cypher = String.format("CREATE (v:%s {id: $vid}), (t:%s {id: $tid})", SOCIAL_USER, SOCIAL_USER);
        neo4jClient.query(cypher)
                .bindAll(Map.of("vid", visitorId, "tid", targetId))
                .run();
    }

    @Test
    @DisplayName("saveTrace (Insert): 관계가 없을 때 호출하면 새로 생성된다")
    void saveTrace_ShouldCreate_WhenNotExists() {
        // Given
        int count = 1;
        LocalDateTime now = LocalDateTime.now();

        // When
        traceRepository.saveTrace(visitorId, targetId, count, now);

        // Then
        // DB에서 다시 조회해서 검증
        Optional<Trace> savedTrace = traceRepository.findByVisitorIdAndTargetId(visitorId, targetId);

        assertThat(savedTrace).isPresent();
        assertThat(savedTrace.get().getCount()).isEqualTo(1);
        assertThat(savedTrace.get().getTarget().getId()).isEqualTo(targetId);
    }

    @Test
    @DisplayName("saveTrace (Update): 이미 관계가 있을 때 호출하면 카운트가 업데이트된다")
    void saveTrace_ShouldUpdate_WhenExists() {
        // Given: 기존에 count=1인 Trace 생성
        LocalDateTime oldTime = LocalDateTime.now().minusDays(1);
        traceRepository.saveTrace(visitorId, targetId, 1, oldTime);

        // When: count=2로 업데이트 요청
        LocalDateTime newTime = LocalDateTime.now();
        traceRepository.saveTrace(visitorId, targetId, 2, newTime);

        // Then
        Trace updatedTrace = traceRepository.findByVisitorIdAndTargetId(visitorId, targetId).get();

        assertThat(updatedTrace.getCount()).isEqualTo(2); // 업데이트 확인
        assertThat(updatedTrace.getLastVisitedAt()).isEqualTo(newTime); // 시간 갱신 확인
    }

    @Test
    @DisplayName("getVisitCount: 관계가 있으면 저장된 count를, 없으면 0을 반환한다")
    void getVisitCount_Test() {
        //  없을 때
        assertThat(traceRepository.getVisitCount(visitorId, targetId)).isEqualTo(0);

        //  있을 때 (count=5 저장)
        traceRepository.saveTrace(visitorId, targetId, 5, LocalDateTime.now());
        assertThat(traceRepository.getVisitCount(visitorId, targetId)).isEqualTo(5);
    }
}