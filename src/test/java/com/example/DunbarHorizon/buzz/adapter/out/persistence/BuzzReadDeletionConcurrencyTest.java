package com.example.DunbarHorizon.buzz.adapter.out.persistence;

import com.example.DunbarHorizon.buzz.application.port.out.BuzzSocialPort;
import com.example.DunbarHorizon.buzz.application.port.out.ImageStoragePort;
import com.example.DunbarHorizon.buzz.application.service.BuzzProfileAssembler;
import com.example.DunbarHorizon.buzz.application.service.BuzzService;
import com.example.DunbarHorizon.buzz.application.service.RecipientStrategyProvider;
import com.example.DunbarHorizon.buzz.domain.Buzz;
import com.example.DunbarHorizon.buzz.domain.BuzzAudienceMember;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzNotFoundException;
import com.example.DunbarHorizon.support.JpaRepositoryTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Propagation;
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
        BuzzService.class})
class BuzzReadDeletionConcurrencyTest {

    private static final long WAIT_SECONDS = 5;

    @Autowired private BuzzService buzzService;
    @Autowired private BuzzAudienceMemberRepositoryAdapter buzzAudienceMemberRepositoryAdapter;
    @Autowired private JdbcTemplate jdbcTemplate;
    @MockitoSpyBean private BuzzRepositoryAdapter buzzRepositoryAdapter;
    @MockitoBean private RecipientStrategyProvider recipientStrategyProvider;
    @MockitoBean private BuzzSocialPort buzzSocialPort;
    @MockitoBean private ImageStoragePort imageStoragePort;

    private final Long creatorId = 21_001L;
    private final Long recipientId = 21_002L;
    private final List<String> createdBuzzIds = new ArrayList<>();

    @AfterEach
    void tearDown() {
        createdBuzzIds.forEach(buzzId -> {
            jdbcTemplate.update("DELETE FROM buzz_audience_members WHERE buzz_id = ?", buzzId);
            jdbcTemplate.update("DELETE FROM buzzes WHERE id = ?", buzzId);
        });
        createdBuzzIds.clear();
    }

    @Test
    @DisplayName("멤버의 열람이 버즈를 공유 락으로 읽은 뒤 삭제가 들어오면 삭제가 열람 커밋을 기다렸다가 멤버까지 지운다")
    void readLocksBuzzFirst_DeletionWaitsAndRemovesMembers() throws InterruptedException {
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
            assertThat(countRows("buzzes", "id", buzz.getId())).isZero();
            assertThat(countRows("buzz_audience_members", "buzz_id", buzz.getId())).isZero();
        } finally {
            continueRead.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("삭제가 버즈 행을 먼저 지우면 열람은 삭제 커밋을 기다렸다가 버즈를 찾지 못하고 멤버를 되살리지 않는다")
    void deletionRemovesBuzzFirst_ReadFailsWithNotFound() throws InterruptedException {
        // given
        Buzz buzz = saveBuzz();
        CountDownLatch buzzDeleted = new CountDownLatch(1);
        CountDownLatch continueDeletion = new CountDownLatch(1);
        CountDownLatch deletionFinished = new CountDownLatch(1);
        CountDownLatch readFinished = new CountDownLatch(1);
        AtomicReference<Throwable> deletionFailure = new AtomicReference<>();
        AtomicReference<Throwable> readFailure = new AtomicReference<>();
        doAnswer(invocation -> {
            invocation.callRealMethod();
            buzzDeleted.countDown();
            await(continueDeletion, "삭제 재개");
            return null;
        }).when(buzzRepositoryAdapter).deleteById(buzz.getId());

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
            assertThat(countRows("buzz_audience_members", "buzz_id", buzz.getId())).isZero();
        } finally {
            continueDeletion.countDown();
            executor.shutdownNow();
        }
    }

    private Buzz saveBuzz() {
        Buzz buzz = Buzz.create(creatorId, "본문", List.of(), List.of(recipientId), LocalDateTime.now());
        buzzRepositoryAdapter.save(buzz);
        buzzAudienceMemberRepositoryAdapter.saveAll(List.of(BuzzAudienceMember.create(buzz.getId(), recipientId)));
        createdBuzzIds.add(buzz.getId());
        return buzz;
    }

    private long countRows(String table, String column, String buzzId) {
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

    private boolean isAnyTransactionWaitingForLock() {
        Integer waiting = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.innodb_trx WHERE trx_state = 'LOCK WAIT'",
                Integer.class);
        Integer lockingQueries = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.processlist WHERE command = 'Query' "
                        + "AND id <> CONNECTION_ID() AND (info LIKE '%for update%' OR info LIKE '%for share%' "
                        + "OR info LIKE '%delete from buzzes%')",
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
