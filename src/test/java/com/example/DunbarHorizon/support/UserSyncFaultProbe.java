package com.example.DunbarHorizon.support;

import com.example.DunbarHorizon.global.event.user.UserSyncCompletedEvent;
import com.example.DunbarHorizon.global.event.user.UserSyncIntegrationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 테스트 전용 장애 지점. DB를 대체하지 않고 실제 트랜잭션의 커밋 직전과 이벤트 전달만 제어한다.
 * 서버의 COMMIT 응답 실패나 네트워크 단절을 재현하는 장치는 아니다.
 */
public class UserSyncFaultProbe implements ApplicationEventPublisher {

    public static final int TIMEOUT_SECONDS = 30;

    public enum Fault {
        NONE, BLOCK_NEO4J_COMMIT, FAIL_NEO4J_BEFORE_COMMIT, DROP_INTEGRATION, DROP_COMPLETION
    }

    private final ApplicationEventPublisher delegate;
    private final Map<Long, Attempt> attemptsByUser = new ConcurrentHashMap<>();
    private final Map<String, Attempt> attemptsByOutbox = new ConcurrentHashMap<>();
    private final BlockingQueue<Boolean> finishedWorkers = new LinkedBlockingQueue<>();

    public UserSyncFaultProbe(ApplicationEventPublisher delegate) {
        this.delegate = delegate;
    }

    public Attempt prepare(long userId, Fault fault) {
        Attempt attempt = new Attempt(userId, fault);
        attemptsByUser.put(userId, attempt);
        return attempt;
    }

    @Override
    public void publishEvent(Object event) {
        if (event instanceof UserSyncIntegrationEvent integration) {
            Attempt attempt = attemptsByUser.get(integration.userId());
            if (attempt != null) {
                attempt.outboxId = integration.outboxId();
                attemptsByOutbox.put(integration.outboxId(), attempt);
                if (attempt.dropIntegration.compareAndSet(true, false)) {
                    attempt.integrationDropped.countDown();
                    return;
                }
            }
        } else if (event instanceof UserSyncCompletedEvent completed) {
            Attempt attempt = attemptsByOutbox.get(completed.outboxId());
            if (attempt != null) {
                attempt.completionPublications.incrementAndGet();
                if (attempt.dropCompletion.compareAndSet(true, false)) {
                    attempt.completionDropped.countDown();
                    return;
                }
            }
        }
        delegate.publishEvent(event);
    }

    public void afterGraphSave(long userId) {
        Attempt attempt = attemptsByUser.get(userId);
        if (attempt == null) {
            return;
        }
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("그래프 저장은 실제 Spring 트랜잭션 안에서 실행되어야 한다.");
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void beforeCommit(boolean readOnly) {
                attempt.beforeCommitReached.countDown();
                try {
                    if (!attempt.allowCommit.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Neo4j 커밋 대기 해제 시간이 초과되었다.");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Neo4j 커밋 대기가 중단되었다.", e);
                }
                if (attempt.failCommit.compareAndSet(true, false)) {
                    throw new InjectedBeforeCommitFailure();
                }
            }

            @Override
            public void afterCompletion(int status) {
                attempt.graphTransactionResults.add(status);
            }
        });
    }

    public Runnable trackWorker(Runnable task) {
        if (task instanceof CleanupBarrier) {
            return task;
        }
        return () -> {
            try {
                task.run();
            } finally {
                finishedWorkers.add(Boolean.TRUE);
            }
        };
    }

    public Runnable cleanupBarrier(CountDownLatch drained) {
        return new CleanupBarrier(drained);
    }

    public boolean awaitWorkerFinished() throws InterruptedException {
        return finishedWorkers.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS) != null;
    }

    public boolean ownsOutbox(String outboxId) {
        return attemptsByOutbox.containsKey(outboxId);
    }

    public Collection<Attempt> attempts() {
        return List.copyOf(attemptsByUser.values());
    }

    public void releaseBlockedCommits() {
        attemptsByUser.values().forEach(Attempt::releaseCommit);
    }

    public void reset() {
        attemptsByUser.clear();
        attemptsByOutbox.clear();
        finishedWorkers.clear();
    }

    public static class Attempt {
        private final long userId;
        private volatile String outboxId;
        private final AtomicBoolean failCommit;
        private final AtomicBoolean dropIntegration;
        private final AtomicBoolean dropCompletion;
        private final CountDownLatch beforeCommitReached = new CountDownLatch(1);
        private final CountDownLatch allowCommit;
        private final CountDownLatch integrationDropped = new CountDownLatch(1);
        private final CountDownLatch completionDropped = new CountDownLatch(1);
        private final AtomicInteger completionPublications = new AtomicInteger();
        private final List<Integer> graphTransactionResults = new CopyOnWriteArrayList<>();

        private Attempt(long userId, Fault fault) {
            this.userId = userId;
            failCommit = new AtomicBoolean(fault == Fault.FAIL_NEO4J_BEFORE_COMMIT);
            dropIntegration = new AtomicBoolean(fault == Fault.DROP_INTEGRATION);
            dropCompletion = new AtomicBoolean(fault == Fault.DROP_COMPLETION);
            allowCommit = new CountDownLatch(fault == Fault.BLOCK_NEO4J_COMMIT ? 1 : 0);
        }

        public long userId() {
            return userId;
        }

        public String outboxId() {
            return outboxId;
        }

        public int completionPublications() {
            return completionPublications.get();
        }

        public List<Integer> graphTransactionResults() {
            return List.copyOf(graphTransactionResults);
        }

        public boolean awaitBeforeCommit() throws InterruptedException {
            return beforeCommitReached.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }

        public boolean awaitIntegrationDropped() throws InterruptedException {
            return integrationDropped.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }

        public boolean awaitCompletionDropped() throws InterruptedException {
            return completionDropped.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }

        public void releaseCommit() {
            allowCommit.countDown();
        }
    }

    private static class InjectedBeforeCommitFailure extends RuntimeException {
        private InjectedBeforeCommitFailure() {
            super("테스트가 Neo4j 커밋 직전에 주입한 실패");
        }
    }

    private record CleanupBarrier(CountDownLatch drained) implements Runnable {
        @Override
        public void run() {
            drained.countDown();
        }
    }
}
