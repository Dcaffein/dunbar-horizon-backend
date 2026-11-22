package com.example.DunbarHorizon.buzz.adapter.out.persistence;

import com.example.DunbarHorizon.buzz.application.dto.command.UpdateBuzzCommand;
import com.example.DunbarHorizon.buzz.application.port.out.BuzzSocialPort;
import com.example.DunbarHorizon.buzz.application.port.out.ImageStoragePort;
import com.example.DunbarHorizon.buzz.application.service.BuzzProfileAssembler;
import com.example.DunbarHorizon.buzz.application.service.BuzzPurgeService;
import com.example.DunbarHorizon.buzz.application.service.BuzzService;
import com.example.DunbarHorizon.buzz.domain.Buzz;
import com.example.DunbarHorizon.buzz.domain.BuzzAudienceMember;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzNotFoundException;
import com.example.DunbarHorizon.support.JpaRepositoryTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;

@JpaRepositoryTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({BuzzRepositoryAdapter.class, BuzzAudienceMemberRepositoryAdapter.class,
        BuzzQueryRepositoryAdapter.class, BuzzThreadRepositoryAdapter.class, BuzzCommentRepositoryAdapter.class, BuzzProfileAssembler.class,
        BuzzService.class, BuzzPurgeService.class})
class BuzzReadDeletionConcurrencyTest {

    private static final long WAIT_SECONDS = 5;

    @Autowired private BuzzService buzzService;
    @Autowired private BuzzPurgeService buzzPurgeService;
    @Autowired private BuzzAudienceMemberRepositoryAdapter buzzAudienceMemberRepositoryAdapter;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private PlatformTransactionManager transactionManager;
    @MockitoSpyBean private BuzzRepositoryAdapter buzzRepositoryAdapter;
    @MockitoBean private BuzzSocialPort buzzSocialPort;
    @MockitoBean private ImageStoragePort imageStoragePort;

    private final Long creatorId = 21_001L;
    private final Long recipientId = 21_002L;
    private final List<Long> createdBuzzIds = new ArrayList<>();

    @AfterEach
    void tearDown() {
        createdBuzzIds.forEach(buzzId -> {
            jdbcTemplate.update("DELETE FROM buzz_audience_members WHERE buzz_id = ?", buzzId);
            jdbcTemplate.update("DELETE FROM buzzes WHERE id = ?", buzzId);
        });
        createdBuzzIds.clear();
    }

    @Test
    @DisplayName("멤버의 열람이 버즈를 공유 락으로 읽은 뒤 삭제가 들어오면 삭제가 열람 커밋을 기다렸다가 삭제 표시를 남기고 정리가 멤버를 지운다")
    void readLocksBuzzFirst_DeletionWaitsThenPurgeRemovesMembers() throws InterruptedException {
        // given
        Buzz buzz = saveBuzz();
        CountDownLatch buzzLocked = new CountDownLatch(1);
        CountDownLatch continueRead = new CountDownLatch(1);
        CountDownLatch readFinished = new CountDownLatch(1);
        CountDownLatch deletionFinished = new CountDownLatch(1);
        AtomicReference<Throwable> readFailure = new AtomicReference<>();
        AtomicReference<Throwable> deletionFailure = new AtomicReference<>();
        AtomicBoolean blockOnce = new AtomicBoolean(true);
        doAnswer(invocation -> {
            Object locked = invocation.callRealMethod();
            if (blockOnce.compareAndSet(true, false)) {
                buzzLocked.countDown();
                await(continueRead, "열람 재개");
            }
            return locked;
        }).when(buzzRepositoryAdapter).findByIdForShare(buzz.getId());

        ExecutorService executor = Executors.newFixedThreadPool(2);
        submit(executor, () -> buzzService.getBuzzDetail(recipientId, buzz.getId()), readFailure, readFinished);

        try {
            // when
            assertThat(buzzLocked.await(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();
            submit(executor, () -> buzzService.deleteBuzz(creatorId, buzz.getId()), deletionFailure, deletionFinished);
            awaitFinishedOrLockWait(deletionFinished, "버즈 삭제");
            assertThat(deletionFinished.getCount()).isEqualTo(1);
            continueRead.countDown();
            assertThat(readFinished.await(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();
            assertThat(deletionFinished.await(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();

            // then
            assertThat(readFailure.get()).isNull();
            assertThat(deletionFailure.get()).isNull();
            assertThat(isDeleted(buzz.getId())).isTrue();
            assertThat(isRead(buzz.getId())).isTrue();

            buzzPurgeService.purgeExpiredAndDeletedBuzzes(LocalDateTime.now());
            assertThat(countRows("buzzes", "id", buzz.getId())).isZero();
            assertThat(countRows("buzz_audience_members", "buzz_id", buzz.getId())).isZero();
        } finally {
            continueRead.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("삭제가 버즈를 배타 락으로 먼저 읽으면 열람은 삭제 커밋을 기다렸다가 버즈를 찾지 못하고 읽음을 남기지 않는다")
    void deletionLocksBuzzFirst_ReadFailsWithNotFound() throws InterruptedException {
        // given
        Buzz buzz = saveBuzz();
        CountDownLatch buzzDeleted = new CountDownLatch(1);
        CountDownLatch continueDeletion = new CountDownLatch(1);
        CountDownLatch deletionFinished = new CountDownLatch(1);
        CountDownLatch readFinished = new CountDownLatch(1);
        AtomicReference<Throwable> deletionFailure = new AtomicReference<>();
        AtomicReference<Throwable> readFailure = new AtomicReference<>();
        AtomicBoolean blockOnce = new AtomicBoolean(true);
        doAnswer(invocation -> {
            Object locked = invocation.callRealMethod();
            if (blockOnce.compareAndSet(true, false)) {
                buzzDeleted.countDown();
                await(continueDeletion, "삭제 재개");
            }
            return locked;
        }).when(buzzRepositoryAdapter).findByIdForUpdate(buzz.getId());

        ExecutorService executor = Executors.newFixedThreadPool(2);
        submit(executor, () -> buzzService.deleteBuzz(creatorId, buzz.getId()), deletionFailure, deletionFinished);

        try {
            // when
            assertThat(buzzDeleted.await(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();
            submit(executor, () -> buzzService.getBuzzDetail(recipientId, buzz.getId()), readFailure, readFinished);
            awaitFinishedOrLockWait(readFinished, "버즈 열람");
            continueDeletion.countDown();
            assertThat(deletionFinished.await(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();
            assertThat(readFinished.await(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();

            // then
            assertThat(deletionFailure.get()).isNull();
            assertThat(readFailure.get()).isInstanceOf(BuzzNotFoundException.class);
            assertThat(isDeleted(buzz.getId())).isTrue();
            assertThat(isRead(buzz.getId())).isFalse();
        } finally {
            continueDeletion.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("수정이 버즈를 읽은 뒤 정리가 버즈 행을 지우면 수정은 동시 수정 실패로 끝나고 버즈를 되살리지 않는다")
    void updateAfterPurgeCommitted_FailsWithoutResurrecting() {
        // given
        Buzz buzz = saveBuzz();
        AtomicBoolean deleteOnce = new AtomicBoolean(true);
        doAnswer(invocation -> {
            Object loaded = invocation.callRealMethod();
            if (deleteOnce.compareAndSet(true, false)) {
                try (var otherConnection = Executors.newSingleThreadExecutor()) {
                    otherConnection.submit(() -> jdbcTemplate.update("DELETE FROM buzzes WHERE id = ?", buzz.getId()))
                            .get(WAIT_SECONDS, TimeUnit.SECONDS);
                }
            }
            return loaded;
        }).when(buzzRepositoryAdapter).findById(buzz.getId());

        // when
        Throwable failure = org.assertj.core.api.Assertions.catchThrowable(() -> buzzService.updateBuzz(
                new UpdateBuzzCommand(creatorId, buzz.getId(), "고친 본문", List.of())));

        // then
        assertThat(failure).isInstanceOf(OptimisticLockingFailureException.class);
        assertThat(countRows("buzzes", "id", buzz.getId())).isZero();
    }

    @Test
    @DisplayName("수정이 버즈를 읽은 뒤 게시자 삭제가 커밋되어도 수정은 삭제 표시를 되돌리지 않는다")
    void updateAfterSoftDeletionCommitted_KeepsDeleted() {
        // given
        Buzz buzz = saveBuzz();
        AtomicBoolean deleteOnce = new AtomicBoolean(true);
        doAnswer(invocation -> {
            Object loaded = invocation.callRealMethod();
            if (deleteOnce.compareAndSet(true, false)) {
                try (var otherTransaction = Executors.newSingleThreadExecutor()) {
                    otherTransaction.submit(() -> buzzService.deleteBuzz(creatorId, buzz.getId()))
                            .get(WAIT_SECONDS, TimeUnit.SECONDS);
                }
            }
            return loaded;
        }).when(buzzRepositoryAdapter).findById(buzz.getId());

        // when
        buzzService.updateBuzz(new UpdateBuzzCommand(creatorId, buzz.getId(), "고친 본문", List.of()));

        // then
        assertThat(isDeleted(buzz.getId())).isTrue();
    }

    private boolean isDeleted(Long buzzId) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT deleted_at IS NOT NULL FROM buzzes WHERE id = ?", Boolean.class, buzzId));
    }

    private boolean isRead(Long buzzId) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT is_read FROM buzz_audience_members WHERE buzz_id = ? AND user_id = ?",
                Boolean.class, buzzId, recipientId));
    }

    private Buzz saveBuzz() {
        Buzz buzz = Buzz.create(creatorId, "본문", List.of(), List.of(recipientId), LocalDateTime.now());
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            buzzRepositoryAdapter.save(buzz);
            buzzAudienceMemberRepositoryAdapter.saveAll(List.of(BuzzAudienceMember.create(buzz.getId(), recipientId)));
        });
        createdBuzzIds.add(buzz.getId());
        return buzz;
    }

    private long countRows(String table, String column, Long buzzId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE " + column + " = ?", Long.class, buzzId);
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
