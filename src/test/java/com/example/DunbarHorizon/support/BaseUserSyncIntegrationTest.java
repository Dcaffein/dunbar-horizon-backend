package com.example.DunbarHorizon.support;

import com.example.DunbarHorizon.account.application.service.UserOutboxRetryService;
import com.example.DunbarHorizon.account.domain.outbox.UserEventOutbox;
import com.example.DunbarHorizon.account.domain.repository.UserEventOutboxRepository;
import com.example.DunbarHorizon.global.event.user.UserActivatedEvent;
import com.example.DunbarHorizon.support.UserSyncFaultProbe.Attempt;
import com.example.DunbarHorizon.support.UserSyncFaultProbe.Fault;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AFTER_COMMIT과 서로 다른 DB의 내구성을 검증하므로 테스트 전체에 @Transactional을 붙이지 않는다.
 * OAuth/Firebase/Redis/스케줄러를 로드하지 않으며 자격증명 없이 Testcontainers만 사용한다.
 */
@SpringJUnitConfig(UserSyncIntegrationTestConfig.class)
@TestPropertySource(properties = {
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.jpa.open-in-view=false",
                "spring.flyway.enabled=true",
                "spring.flyway.baseline-on-migrate=true",
                "spring.flyway.baseline-version=1",
                "spring.flyway.locations=classpath:db/migration",
                "spring.data.neo4j.database=neo4j"
})
public abstract class BaseUserSyncIntegrationTest {

    @Autowired protected UserSyncFaultProbe faults;
    @Autowired private UserEventOutboxRepository outboxRepository;
    @Autowired private UserOutboxRetryService retryService;
    @Autowired private ApplicationContext applicationContext;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private Neo4jClient neo4j;
    @Autowired private ThreadPoolTaskExecutor taskExecutor;
    @Autowired @Qualifier("transactionManager") private PlatformTransactionManager mysqlTransactions;

    @BeforeEach
    void resetFaults() {
        faults.reset();
    }

    @AfterEach
    void removeOnlyThisTestsFixtures() throws InterruptedException {
        faults.releaseBlockedCommits();
        // 단일 작업 큐의 끝까지 기다린 뒤 정리한다. 실패한 테스트도 실행 중인 작업과 정리가 경합하지 않는다.
        // sentinel은 worker 완료 큐에 기록하지 않아 다음 테스트의 완료 신호로 잘못 소비되지 않는다.
        CountDownLatch drained = new CountDownLatch(1);
        taskExecutor.execute(faults.cleanupBarrier(drained));
        assertThat(drained.await(UserSyncFaultProbe.TIMEOUT_SECONDS, TimeUnit.SECONDS))
                .as("fixture 정리 전 비동기 작업 큐 종료").isTrue();
        for (Attempt attempt : faults.attempts()) {
            neo4j.query("MATCH (u:SocialUser {id: $id}) DETACH DELETE u")
                    .bind(attempt.userId()).to("id").run();
            if (attempt.outboxId() != null) {
                jdbc.update("DELETE FROM user_event_outboxes WHERE id = ?", attempt.outboxId());
            }
            jdbc.update("DELETE FROM users WHERE user_id = ?", attempt.userId());
        }
    }

    protected Attempt activateUser(Fault fault) {
        // 다른 테스트의 양수 고정 그래프 ID와 충돌하지 않는 fixture를 실제 MySQL 트랜잭션으로 만든다.
        long userId = -ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE);
        assertThat(graphNodeCount(userId)).isZero();
        Attempt attempt = faults.prepare(userId, fault);
        new TransactionTemplate(mysqlTransactions).executeWithoutResult(status -> {
            jdbc.update("""
                    INSERT INTO users (user_id, email, nickname, role, status, created_at, updated_at)
                    VALUES (?, ?, ?, 'USER', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """, userId, "sync" + Long.toUnsignedString(userId) + "@test.invalid", "sync-boundary");
            applicationContext.publishEvent(new UserActivatedEvent(
                    userId, "sync-boundary", "https://example.invalid/sync.png"
            ));
        });
        assertThat(attempt.outboxId()).as("BEFORE_COMMIT 리스너가 생성한 Outbox ID").isNotNull();
        return attempt;
    }

    protected void awaitWorkerFinished() throws InterruptedException {
        assertThat(faults.awaitWorkerFinished()).as("비동기 동기화 작업의 종료 신호").isTrue();
    }

    protected UserEventOutbox outbox(Attempt attempt) {
        // 테스트에는 외부 트랜잭션/영속성 컨텍스트가 없으므로 매번 새 JPA 조회로 커밋된 상태를 읽는다.
        return outboxRepository.findById(attempt.outboxId()).orElseThrow();
    }

    protected boolean sourceUserExists(Attempt attempt) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE user_id = ?", Long.class, attempt.userId()) == 1L;
    }

    protected long graphNodeCount(Attempt attempt) {
        return graphNodeCount(attempt.userId());
    }

    private long graphNodeCount(long userId) {
        return neo4j.query("MATCH (u:SocialUser {id: $id}) RETURN count(u)")
                .bind(userId).to("id").fetchAs(Long.class).one().orElseThrow();
    }

    protected void retryAfterPendingThreshold(Attempt attempt) throws InterruptedException {
        // 5분을 실제로 기다리지 않고 fixture 생성 시각만 과거로 이동한다. 재시도 서비스/조회는 실제 코드다.
        assertThat(jdbc.update("UPDATE user_event_outboxes SET created_at = ? WHERE id = ?",
                Timestamp.valueOf(LocalDateTime.now().minusMinutes(6)), attempt.outboxId())).isEqualTo(1);
        retryService.retryPending();
        awaitWorkerFinished();
    }
}
