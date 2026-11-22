package com.example.DunbarHorizon.buzz.adapter.out.persistence;

import com.example.DunbarHorizon.buzz.application.dto.command.WriteBuzzCommentCommand;
import com.example.DunbarHorizon.buzz.application.port.out.BuzzSocialPort;
import com.example.DunbarHorizon.buzz.application.port.out.ImageStoragePort;
import com.example.DunbarHorizon.buzz.application.service.BuzzCommentService;
import com.example.DunbarHorizon.buzz.application.service.BuzzProfileAssembler;
import com.example.DunbarHorizon.buzz.application.service.BuzzThreadAccess;
import com.example.DunbarHorizon.buzz.application.service.BuzzThreadService;
import com.example.DunbarHorizon.buzz.domain.Buzz;
import com.example.DunbarHorizon.buzz.domain.BuzzAudienceMember;
import com.example.DunbarHorizon.buzz.domain.BuzzThread;
import com.example.DunbarHorizon.buzz.domain.ThreadVisibility;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzThreadNotFoundException;
import com.example.DunbarHorizon.support.JpaRepositoryTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.annotation.Transactional;

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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;

@JpaRepositoryTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({BuzzRepositoryAdapter.class, BuzzAudienceMemberRepositoryAdapter.class, BuzzThreadRepositoryAdapter.class,
        BuzzCommentRepositoryAdapter.class, BuzzProfileAssembler.class, BuzzThreadAccess.class,
        BuzzThreadService.class, BuzzCommentService.class})
class BuzzCommentConcurrencyTest {

    private static final long WAIT_SECONDS = 5;

    @Autowired private BuzzThreadService buzzThreadService;
    @Autowired private BuzzCommentService buzzCommentService;
    @Autowired private BuzzRepositoryAdapter buzzRepositoryAdapter;
    @Autowired private BuzzAudienceMemberRepositoryAdapter buzzAudienceMemberRepositoryAdapter;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private PlatformTransactionManager transactionManager;
    @MockitoSpyBean private BuzzThreadRepositoryAdapter buzzThreadRepositoryAdapter;
    @MockitoSpyBean private BuzzCommentRepositoryAdapter buzzCommentRepositoryAdapter;
    @MockitoBean private BuzzSocialPort buzzSocialPort;
    @MockitoBean private ImageStoragePort imageStoragePort;

    private final Long creatorId = 24_001L;
    private final Long starterId = 24_002L;
    private final List<String> createdBuzzIds = new ArrayList<>();

    @AfterEach
    void tearDown() {
        createdBuzzIds.forEach(buzzId -> {
            jdbcTemplate.update("DELETE FROM buzz_comments WHERE thread_id IN "
                    + "(SELECT id FROM buzz_threads WHERE buzz_id = ?)", buzzId);
            jdbcTemplate.update("DELETE FROM buzz_threads WHERE buzz_id = ?", buzzId);
            jdbcTemplate.update("DELETE FROM buzz_audience_members WHERE buzz_id = ?", buzzId);
            jdbcTemplate.update("DELETE FROM buzzes WHERE id = ?", buzzId);
        });
        createdBuzzIds.clear();
    }

    @Test
    @DisplayName("답글 쓰기가 스레드를 공유 락으로 읽은 뒤 스레드 삭제가 들어오면 삭제가 기다렸다가 새 답글까지 지운다")
    void commentLocksThreadFirst_DeletionRemovesComment() throws InterruptedException {
        // given
        BuzzThread thread = saveBuzzAndThread();
        CountDownLatch threadLocked = new CountDownLatch(1);
        CountDownLatch continueComment = new CountDownLatch(1);
        CountDownLatch commentFinished = new CountDownLatch(1);
        CountDownLatch deletionFinished = new CountDownLatch(1);
        AtomicReference<Throwable> commentFailure = new AtomicReference<>();
        AtomicReference<Throwable> deletionFailure = new AtomicReference<>();
        AtomicBoolean blockOnce = new AtomicBoolean(true);
        doAnswer(invocation -> {
            Object locked = invocation.callRealMethod();
            if (blockOnce.compareAndSet(true, false)) {
                threadLocked.countDown();
                await(continueComment, "답글 쓰기 재개");
            }
            return locked;
        }).when(buzzThreadRepositoryAdapter).findByIdForShare(thread.getId());

        ExecutorService executor = Executors.newFixedThreadPool(2);
        submit(executor, () -> buzzCommentService.writeComment(command(thread)), commentFailure, commentFinished);

        try {
            // when
            assertThat(threadLocked.await(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();
            submit(executor, () -> buzzThreadService.deleteThread(starterId, thread.getBuzzId(), thread.getId()),
                    deletionFailure, deletionFinished);
            awaitFinishedOrLockWait(deletionFinished, "스레드 삭제");
            assertThat(deletionFinished.getCount()).isEqualTo(1);
            continueComment.countDown();
            assertThat(commentFinished.await(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();
            assertThat(deletionFinished.await(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();

            // then
            assertThat(commentFailure.get()).isNull();
            assertThat(deletionFailure.get()).isNull();
            assertThat(countComments(thread.getId())).isZero();
        } finally {
            continueComment.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("스레드 삭제가 스레드 행을 먼저 지우면 답글 쓰기는 기다렸다가 스레드를 찾지 못한다")
    void deletionRemovesThreadFirst_CommentFailsWithNotFound() throws InterruptedException {
        // given
        BuzzThread thread = saveBuzzAndThread();
        CountDownLatch threadDeleted = new CountDownLatch(1);
        CountDownLatch continueDeletion = new CountDownLatch(1);
        CountDownLatch deletionFinished = new CountDownLatch(1);
        CountDownLatch commentFinished = new CountDownLatch(1);
        AtomicReference<Throwable> deletionFailure = new AtomicReference<>();
        AtomicReference<Throwable> commentFailure = new AtomicReference<>();
        doAnswer(invocation -> {
            invocation.callRealMethod();
            threadDeleted.countDown();
            await(continueDeletion, "스레드 삭제 재개");
            return null;
        }).when(buzzThreadRepositoryAdapter).deleteById(thread.getId());

        ExecutorService executor = Executors.newFixedThreadPool(2);
        submit(executor, () -> buzzThreadService.deleteThread(starterId, thread.getBuzzId(), thread.getId()),
                deletionFailure, deletionFinished);

        try {
            // when
            assertThat(threadDeleted.await(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();
            submit(executor, () -> buzzCommentService.writeComment(command(thread)), commentFailure, commentFinished);
            awaitFinishedOrLockWait(commentFinished, "답글 쓰기");
            continueDeletion.countDown();
            assertThat(deletionFinished.await(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();
            assertThat(commentFinished.await(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();

            // then
            assertThat(deletionFailure.get()).isNull();
            assertThat(commentFailure.get()).isInstanceOf(BuzzThreadNotFoundException.class);
            assertThat(countComments(thread.getId())).isZero();
        } finally {
            continueDeletion.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("같은 스레드에 두 사람이 동시에 답글을 쓰면 서로 덮어쓰지 않고 둘 다 남는다")
    void concurrentComments_BothSaved() throws InterruptedException {
        // given
        BuzzThread thread = saveBuzzAndThread();
        CyclicBarrier bothLocked = new CyclicBarrier(2);
        doAnswer(invocation -> {
            Object locked = invocation.callRealMethod();
            bothLocked.await(WAIT_SECONDS, TimeUnit.SECONDS);
            return locked;
        }).when(buzzThreadRepositoryAdapter).findByIdForShare(thread.getId());
        CountDownLatch finished = new CountDownLatch(2);
        AtomicReference<Throwable> starterFailure = new AtomicReference<>();
        AtomicReference<Throwable> creatorFailure = new AtomicReference<>();

        // when
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            submit(executor, () -> buzzCommentService.writeComment(command(thread)), starterFailure, finished);
            submit(executor, () -> buzzCommentService.writeComment(new WriteBuzzCommentCommand(
                    creatorId, thread.getBuzzId(), thread.getId(), "게시자 답글", List.of())), creatorFailure, finished);
            assertThat(finished.await(WAIT_SECONDS * 2, TimeUnit.SECONDS)).isTrue();
        } finally {
            executor.shutdownNow();
        }

        // then
        assertThat(starterFailure.get()).isNull();
        assertThat(creatorFailure.get()).isNull();
        assertThat(countComments(thread.getId())).isEqualTo(2);
    }

    @Test
    @DisplayName("답글 저장이 실패하면 그 요청의 읽음 변경도 반영되지 않는다")
    void commentSaveFails_ReadChangeRolledBack() {
        // given
        BuzzThread thread = saveBuzzAndThread();
        doThrow(new IllegalStateException("답글 저장 실패")).when(buzzCommentRepositoryAdapter).save(any());

        // when & then
        assertThatThrownBy(() -> buzzCommentService.writeComment(command(thread)))
                .isInstanceOf(IllegalStateException.class);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT is_read FROM buzz_audience_members WHERE buzz_id = ? AND user_id = ?",
                Boolean.class, thread.getBuzzId(), starterId)).isFalse();
    }

    private BuzzThread saveBuzzAndThread() {
        Buzz buzz = Buzz.create(creatorId, "본문", List.of(), List.of(starterId), LocalDateTime.now());
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            buzzRepositoryAdapter.save(buzz);
            buzzAudienceMemberRepositoryAdapter.saveAll(List.of(BuzzAudienceMember.create(buzz.getId(), starterId)));
        });
        createdBuzzIds.add(buzz.getId());
        return buzzThreadRepositoryAdapter.save(BuzzThread.create(
                buzz.getId(), starterId, ThreadVisibility.PUBLIC, "첫 글", List.of(), LocalDateTime.now()));
    }

    private WriteBuzzCommentCommand command(BuzzThread thread) {
        return new WriteBuzzCommentCommand(starterId, thread.getBuzzId(), thread.getId(), "답글", List.of());
    }

    private long countComments(Long threadId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM buzz_comments WHERE thread_id = ?", Long.class, threadId);
        return count == null ? 0 : count;
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

    // PK 등치 DELETE는 잠금을 기다려도 innodb_trx에 LOCK WAIT로 드러나지 않을 때가 있다.
    // 잠금 시스템의 대기 목록(data_lock_waits)을 먼저 보고, 실행 중인 잠금 조회·삭제 문장도 대기로 본다.
    private boolean isAnyTransactionWaitingForLock() {
        Integer lockWaits = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM performance_schema.data_lock_waits", Integer.class);
        Integer lockingQueries = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.processlist WHERE command = 'Query' "
                        + "AND id <> CONNECTION_ID() AND (info LIKE '%for update%' OR info LIKE '%for share%' "
                        + "OR info LIKE 'delete %')",
                Integer.class);
        return (lockWaits != null && lockWaits > 0) || (lockingQueries != null && lockingQueries > 0);
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
