package com.example.DunbarHorizon.flag.domain.comment;

import com.example.DunbarHorizon.flag.adapter.out.persistence.jpa.FlagCommentJpaRepository;
import com.example.DunbarHorizon.flag.adapter.out.persistence.jpa.FlagJpaRepository;
import com.example.DunbarHorizon.flag.application.port.in.FlagCommentCommandUseCase;
import com.example.DunbarHorizon.flag.application.port.in.FlagHostUseCase;
import com.example.DunbarHorizon.flag.application.port.in.command.FlagHostCommand;
import com.example.DunbarHorizon.flag.domain.comment.exception.FlagCommentNotFoundException;
import com.example.DunbarHorizon.flag.domain.comment.repository.FlagCommentRepository;
import com.example.DunbarHorizon.support.TestContainerConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestContainerConfig.class)
class FlagReplyParentLockConcurrencyIntegrationTest {

    private static final Long HOST_ID = 13201L;
    private static final Long REPLIER_ID = 13202L;
    private static final long WAIT_SECONDS = 5;

    @Autowired private FlagHostUseCase flagHostUseCase;
    @Autowired private FlagCommentCommandUseCase commentCommandUseCase;
    @Autowired private FlagJpaRepository flagJpaRepository;
    @Autowired private FlagCommentJpaRepository commentJpaRepository;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private JdbcTemplate jdbcTemplate;

    @MockitoSpyBean private FlagCommentRepository commentRepository;

    private final List<Long> createdFlagIds = new ArrayList<>();

    @AfterEach
    void tearDown() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            if (createdFlagIds.isEmpty()) {
                return;
            }
            commentJpaRepository.hardDeleteByFlagIdsIn(createdFlagIds);
            flagJpaRepository.hardDeleteByIdsIn(createdFlagIds);
        });
        createdFlagIds.clear();
    }

    @Test
    @DisplayName("답글 작성이 부모를 읽은 뒤 부모 삭제가 들어오면 삭제가 답글 커밋을 기다렸다가 함께 지워 고아 답글이 남지 않는다")
    void replyReadsParentFirst_deletionWaitsAndRemovesReply() throws InterruptedException {
        // given
        Long flagId = hostFlag();
        Long parentId = commentCommandUseCase.createRootComment(flagId, HOST_ID, "부모 댓글", false);
        CountDownLatch parentRead = new CountDownLatch(1);
        CountDownLatch continueReply = new CountDownLatch(1);
        CountDownLatch replyFinished = new CountDownLatch(1);
        CountDownLatch deletionFinished = new CountDownLatch(1);
        AtomicReference<Throwable> replyFailure = new AtomicReference<>();
        AtomicReference<Throwable> deletionFailure = new AtomicReference<>();

        pauseOnceAfterParentRead(parentId, parentRead, continueReply);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        submit(executor, () -> commentCommandUseCase.createReply(flagId, parentId, REPLIER_ID, "답글", false),
                replyFailure, replyFinished);

        try {
            // when
            assertThat(parentRead.await(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();
            submit(executor, () -> commentCommandUseCase.deleteComment(flagId, parentId, HOST_ID),
                    deletionFailure, deletionFinished);
            awaitFinishedOrLockWait(deletionFinished, "부모 댓글 삭제");
            continueReply.countDown();
            assertThat(replyFinished.await(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();
            assertThat(deletionFinished.await(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();

            // then
            assertThat(replyFailure.get()).isNull();
            assertThat(deletionFailure.get()).isNull();
            assertThat(commentJpaRepository.countByFlagId(flagId)).isZero();
        } finally {
            continueReply.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("부모 삭제가 먼저 잠그면 답글 작성은 삭제 커밋을 기다렸다가 부모가 없어 실패한다")
    void deletionLocksParentFirst_replyFailsWithNotFound() throws InterruptedException {
        // given
        Long flagId = hostFlag();
        Long parentId = commentCommandUseCase.createRootComment(flagId, HOST_ID, "부모 댓글", false);
        CountDownLatch parentLocked = new CountDownLatch(1);
        CountDownLatch continueDeletion = new CountDownLatch(1);
        CountDownLatch deletionFinished = new CountDownLatch(1);
        CountDownLatch replyFinished = new CountDownLatch(1);
        AtomicBoolean blockOnce = new AtomicBoolean(true);
        AtomicReference<Throwable> deletionFailure = new AtomicReference<>();
        AtomicReference<Throwable> replyFailure = new AtomicReference<>();

        doAnswer(invocation -> {
            Object locked = invocation.callRealMethod();
            if (blockOnce.compareAndSet(true, false)) {
                parentLocked.countDown();
                await(continueDeletion, "부모 댓글 삭제 재개");
            }
            return locked;
        }).when(commentRepository).findByIdForUpdate(parentId);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        submit(executor, () -> commentCommandUseCase.deleteComment(flagId, parentId, HOST_ID),
                deletionFailure, deletionFinished);

        try {
            // when
            assertThat(parentLocked.await(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();
            submit(executor, () -> commentCommandUseCase.createReply(flagId, parentId, REPLIER_ID, "답글", false),
                    replyFailure, replyFinished);
            awaitFinishedOrLockWait(replyFinished, "답글 작성");
            continueDeletion.countDown();
            assertThat(deletionFinished.await(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();
            assertThat(replyFinished.await(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();

            // then
            assertThat(deletionFailure.get()).isNull();
            assertThat(replyFailure.get()).isInstanceOf(FlagCommentNotFoundException.class);
            assertThat(commentJpaRepository.countByFlagId(flagId)).isZero();
        } finally {
            continueDeletion.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("같은 부모 댓글에 답글 둘을 동시에 작성하면 서로 기다리지 않고 둘 다 성공한다")
    void twoRepliesOnSameParent_bothSucceed() throws InterruptedException {
        // given
        Long flagId = hostFlag();
        Long parentId = commentCommandUseCase.createRootComment(flagId, HOST_ID, "부모 댓글", false);
        CyclicBarrier bothRead = new CyclicBarrier(2);
        CountDownLatch finished = new CountDownLatch(2);
        AtomicReference<Throwable> firstFailure = new AtomicReference<>();
        AtomicReference<Throwable> secondFailure = new AtomicReference<>();

        doAnswer(invocation -> {
            Object parent = invocation.callRealMethod();
            bothRead.await(WAIT_SECONDS, TimeUnit.SECONDS);
            return parent;
        }).when(commentRepository).findByIdForShare(parentId);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        submit(executor, () -> commentCommandUseCase.createReply(flagId, parentId, REPLIER_ID, "첫 답글", false),
                firstFailure, finished);
        submit(executor, () -> commentCommandUseCase.createReply(flagId, parentId, HOST_ID, "둘째 답글", false),
                secondFailure, finished);

        try {
            // when
            assertThat(finished.await(WAIT_SECONDS * 2, TimeUnit.SECONDS)).isTrue();

            // then
            assertThat(firstFailure.get()).isNull();
            assertThat(secondFailure.get()).isNull();
            assertThat(commentJpaRepository.countByFlagId(flagId)).isEqualTo(3);
        } finally {
            executor.shutdownNow();
        }
    }

    private void pauseOnceAfterParentRead(Long parentId, CountDownLatch parentRead, CountDownLatch continueReply) {
        AtomicBoolean blockOnce = new AtomicBoolean(true);
        Answer<Object> pause = invocation -> {
            Object parent = invocation.callRealMethod();
            if (blockOnce.compareAndSet(true, false)) {
                parentRead.countDown();
                await(continueReply, "답글 작성 재개");
            }
            return parent;
        };
        doAnswer(pause).when(commentRepository).findByIdForShare(parentId);
        doAnswer(pause).when(commentRepository).findById(parentId);
    }

    private Long hostFlag() {
        LocalDateTime base = LocalDateTime.now().withNano(0);
        Long flagId = flagHostUseCase.hostFlag(new FlagHostCommand(
                HOST_ID, "답글 잠금 테스트", "설명", 5,
                base.plusHours(1), base.plusHours(2), base.plusHours(3)));
        createdFlagIds.add(flagId);
        return flagId;
    }

    private void submit(ExecutorService executor, Runnable action,
                        AtomicReference<Throwable> failure, CountDownLatch finished) {
        executor.submit(() -> {
            try {
                action.run();
            } catch (Throwable throwable) {
                failure.set(throwable);
            } finally {
                finished.countDown();
            }
        });
    }

    private void awaitFinishedOrLockWait(CountDownLatch finished, String action) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(WAIT_SECONDS);
        while (System.nanoTime() < deadline) {
            if (finished.await(50, TimeUnit.MILLISECONDS) || isAnyTransactionWaitingForLock()) {
                return;
            }
        }
        throw new AssertionError(action + "이 " + WAIT_SECONDS + "초 안에 끝나지도 잠금 대기에 들어가지도 않았습니다.");
    }

    // PK 등치 잠금 조회는 최적화 단계(statistics)에서 행을 읽으며 잠금을 기다리는데, 이때
    // innodb_trx는 LOCK WAIT로 바로 드러나지 않는다. 실행 중인 잠금 조회도 대기로 본다.
    private boolean isAnyTransactionWaitingForLock() {
        Integer waiting = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.innodb_trx WHERE trx_state = 'LOCK WAIT'",
                Integer.class);
        Integer lockingQueries = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.processlist WHERE command = 'Query' "
                        + "AND id <> CONNECTION_ID() AND (info LIKE '%for update%' OR info LIKE '%for share%')",
                Integer.class);
        return (waiting != null && waiting > 0) || (lockingQueries != null && lockingQueries > 0);
    }

    private static void await(CountDownLatch latch, String action) {
        try {
            if (!latch.await(WAIT_SECONDS, TimeUnit.SECONDS)) {
                throw new AssertionError(action + "이 " + WAIT_SECONDS + "초 안에 완료되지 않았습니다.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(action + " 대기 중 인터럽트되었습니다.", e);
        }
    }
}
