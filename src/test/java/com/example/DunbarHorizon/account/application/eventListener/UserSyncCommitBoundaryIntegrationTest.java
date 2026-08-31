package com.example.DunbarHorizon.account.application.eventListener;

import com.example.DunbarHorizon.account.domain.outbox.UserOutboxStatus;
import com.example.DunbarHorizon.support.BaseUserSyncIntegrationTest;
import com.example.DunbarHorizon.support.UserSyncFaultProbe.Attempt;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static com.example.DunbarHorizon.support.UserSyncFaultProbe.Fault.BLOCK_NEO4J_COMMIT;
import static com.example.DunbarHorizon.support.UserSyncFaultProbe.Fault.DROP_COMPLETION;
import static com.example.DunbarHorizon.support.UserSyncFaultProbe.Fault.DROP_INTEGRATION;
import static com.example.DunbarHorizon.support.UserSyncFaultProbe.Fault.FAIL_NEO4J_BEFORE_COMMIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.transaction.support.TransactionSynchronization.STATUS_COMMITTED;
import static org.springframework.transaction.support.TransactionSynchronization.STATUS_ROLLED_BACK;

@Execution(ExecutionMode.SAME_THREAD)
class UserSyncCommitBoundaryIntegrationTest extends BaseUserSyncIntegrationTest {

    @Test
    @DisplayName("Neo4j 커밋 직전에는 PENDING이며 커밋 성공 후에만 COMPLETED가 된다")
    void completesOutboxOnlyAfterNeo4jCommit() throws InterruptedException {
        // given
        Attempt attempt = activateUser(BLOCK_NEO4J_COMMIT);

        // when
        assertThat(attempt.awaitBeforeCommit()).as("실제 Neo4j 트랜잭션의 커밋 직전 도달").isTrue();

        // then
        assertThat(sourceUserExists(attempt)).isTrue();
        assertThat(outbox(attempt).getStatus()).isEqualTo(UserOutboxStatus.PENDING);
        assertThat(graphNodeCount(attempt)).isZero();
        assertThat(attempt.completionPublications()).isZero();

        attempt.releaseCommit();
        awaitWorkerFinished();
        assertThat(attempt.graphTransactionResults()).containsExactly(STATUS_COMMITTED);
        assertThat(graphNodeCount(attempt)).isEqualTo(1);
        assertThat(outbox(attempt).getStatus()).isEqualTo(UserOutboxStatus.COMPLETED);
        assertThat(outbox(attempt).getRetryCount()).isZero();
    }

    @Test
    @DisplayName("Neo4j 커밋 직전 예외는 그래프를 롤백하고 PENDING을 남겨 실제 재시도로 복구한다")
    void rollsBackOnBeforeCommitFailureThenRetries() throws InterruptedException {
        // given
        Attempt attempt = activateUser(FAIL_NEO4J_BEFORE_COMMIT);

        // when
        awaitWorkerFinished();

        // then
        assertThat(sourceUserExists(attempt)).isTrue();
        assertThat(attempt.graphTransactionResults()).containsExactly(STATUS_ROLLED_BACK);
        assertThat(attempt.completionPublications()).isZero();
        assertThat(graphNodeCount(attempt)).isZero();
        assertThat(outbox(attempt).getStatus()).isEqualTo(UserOutboxStatus.PENDING);

        retryAfterPendingThreshold(attempt);
        assertThat(attempt.graphTransactionResults()).containsExactly(STATUS_ROLLED_BACK, STATUS_COMMITTED);
        assertThat(graphNodeCount(attempt)).isEqualTo(1);
        assertThat(outbox(attempt).getStatus()).isEqualTo(UserOutboxStatus.COMPLETED);
        assertThat(outbox(attempt).getRetryCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("최초 동기화 이벤트가 전달되지 않아도 원본과 PENDING을 보존하고 실제 재시도로 복구한다")
    void retriesWhenInitialIntegrationDeliveryIsLost() throws InterruptedException {
        // given
        Attempt attempt = activateUser(DROP_INTEGRATION);

        // when
        assertThat(attempt.awaitIntegrationDropped()).as("최초 이벤트 전달 누락 지점 도달").isTrue();

        // then
        assertThat(sourceUserExists(attempt)).isTrue();
        assertThat(outbox(attempt).getStatus()).isEqualTo(UserOutboxStatus.PENDING);
        assertThat(graphNodeCount(attempt)).isZero();
        assertThat(attempt.graphTransactionResults()).isEmpty();

        retryAfterPendingThreshold(attempt);
        assertThat(attempt.graphTransactionResults()).containsExactly(STATUS_COMMITTED);
        assertThat(graphNodeCount(attempt)).isEqualTo(1);
        assertThat(outbox(attempt).getStatus()).isEqualTo(UserOutboxStatus.COMPLETED);
        assertThat(outbox(attempt).getRetryCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("완료 이벤트 전달이 누락되면 PENDING으로 재시도하되 그래프 노드는 중복 생성하지 않는다")
    void retriesIdempotentlyWhenCompletionDeliveryIsLost() throws InterruptedException {
        // given
        Attempt attempt = activateUser(DROP_COMPLETION);

        // when
        awaitWorkerFinished();

        // then
        assertThat(attempt.awaitCompletionDropped()).as("완료 이벤트 전달 누락 지점 도달").isTrue();
        assertThat(attempt.graphTransactionResults()).containsExactly(STATUS_COMMITTED);
        assertThat(graphNodeCount(attempt)).isEqualTo(1);
        assertThat(outbox(attempt).getStatus()).isEqualTo(UserOutboxStatus.PENDING);

        retryAfterPendingThreshold(attempt);
        assertThat(attempt.graphTransactionResults()).containsExactly(STATUS_COMMITTED, STATUS_COMMITTED);
        assertThat(attempt.completionPublications()).isEqualTo(2);
        assertThat(graphNodeCount(attempt)).isEqualTo(1);
        assertThat(outbox(attempt).getStatus()).isEqualTo(UserOutboxStatus.COMPLETED);
        assertThat(outbox(attempt).getRetryCount()).isEqualTo(1);
    }
}
