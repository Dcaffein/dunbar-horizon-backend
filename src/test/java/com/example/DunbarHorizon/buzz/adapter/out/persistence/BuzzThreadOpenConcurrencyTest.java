package com.example.DunbarHorizon.buzz.adapter.out.persistence;

import com.example.DunbarHorizon.buzz.application.dto.command.OpenBuzzThreadCommand;
import com.example.DunbarHorizon.buzz.application.port.out.BuzzSocialPort;
import com.example.DunbarHorizon.buzz.application.port.out.ImageStoragePort;
import com.example.DunbarHorizon.buzz.application.service.BuzzProfileAssembler;
import com.example.DunbarHorizon.buzz.application.service.BuzzService;
import com.example.DunbarHorizon.buzz.application.service.BuzzThreadService;
import com.example.DunbarHorizon.buzz.application.service.RecipientStrategyProvider;
import com.example.DunbarHorizon.buzz.domain.Buzz;
import com.example.DunbarHorizon.buzz.domain.BuzzAudienceMember;
import com.example.DunbarHorizon.buzz.domain.BuzzThread;
import com.example.DunbarHorizon.buzz.domain.ThreadVisibility;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzNotFoundException;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzThreadAlreadyOpenedException;
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
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;

@JpaRepositoryTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({BuzzRepositoryAdapter.class, BuzzAudienceMemberRepositoryAdapter.class, BuzzQueryRepositoryAdapter.class,
        BuzzThreadRepositoryAdapter.class, BuzzProfileAssembler.class, BuzzService.class, BuzzThreadService.class})
class BuzzThreadOpenConcurrencyTest {

    private static final long WAIT_SECONDS = 5;

    @Autowired private BuzzService buzzService;
    @Autowired private BuzzThreadService buzzThreadService;
    @Autowired private BuzzThreadRepositoryAdapter buzzThreadRepositoryAdapter;
    @Autowired private JdbcTemplate jdbcTemplate;
    @MockitoSpyBean private BuzzRepositoryAdapter buzzRepositoryAdapter;
    @MockitoSpyBean private BuzzAudienceMemberRepositoryAdapter buzzAudienceMemberRepositoryAdapter;
    @MockitoBean private RecipientStrategyProvider recipientStrategyProvider;
    @MockitoBean private BuzzSocialPort buzzSocialPort;
    @MockitoBean private ImageStoragePort imageStoragePort;

    private final Long creatorId = 23_001L;
    private final Long starterId = 23_002L;
    private final List<String> createdBuzzIds = new ArrayList<>();

    @AfterEach
    void tearDown() {
        createdBuzzIds.forEach(buzzId -> {
            jdbcTemplate.update("DELETE FROM buzz_threads WHERE buzz_id = ?", buzzId);
            jdbcTemplate.update("DELETE FROM buzz_audience_members WHERE buzz_id = ?", buzzId);
            jdbcTemplate.update("DELETE FROM buzzes WHERE id = ?", buzzId);
        });
        createdBuzzIds.clear();
    }

    @Test
    @DisplayName("같은 멤버가 같은 공개 범위의 스레드를 동시에 열면 하나만 저장되고 다른 요청은 중복으로 거절된다")
    void concurrentSameVisibility_OnlyOneSaved() throws InterruptedException {
        // given
        Buzz buzz = saveBuzz();
        CyclicBarrier bothChecked = new CyclicBarrier(2);
        doAnswer(invocation -> {
            Object member = invocation.callRealMethod();
            bothChecked.await(WAIT_SECONDS, TimeUnit.SECONDS);
            return member;
        }).when(buzzAudienceMemberRepositoryAdapter).findByBuzzIdAndUserId(buzz.getId(), starterId);
        CountDownLatch finished = new CountDownLatch(2);
        AtomicReference<Throwable> firstFailure = new AtomicReference<>();
        AtomicReference<Throwable> secondFailure = new AtomicReference<>();

        // when
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            submit(executor, () -> buzzThreadService.openThread(command(buzz, ThreadVisibility.PUBLIC)), firstFailure, finished);
            submit(executor, () -> buzzThreadService.openThread(command(buzz, ThreadVisibility.PUBLIC)), secondFailure, finished);
            assertThat(finished.await(WAIT_SECONDS * 2, TimeUnit.SECONDS)).isTrue();
        } finally {
            executor.shutdownNow();
        }

        // then
        List<Throwable> failures = new ArrayList<>();
        if (firstFailure.get() != null) failures.add(firstFailure.get());
        if (secondFailure.get() != null) failures.add(secondFailure.get());
        assertThat(failures).hasSize(1).first().isInstanceOf(BuzzThreadAlreadyOpenedException.class);
        assertThat(countRows("buzz_threads", "buzz_id", buzz.getId())).isEqualTo(1);
    }

    @Test
    @DisplayName("스레드 저장이 중복으로 실패하면 그 요청의 읽음 변경도 반영되지 않는다")
    void duplicateOpen_ReadChangeRolledBack() {
        // given
        Buzz buzz = saveBuzz();
        buzzThreadRepositoryAdapter.save(BuzzThread.open(
                buzz.getId(), starterId, ThreadVisibility.PRIVATE, "먼저 연 글", List.of(), LocalDateTime.now()));

        // when & then
        assertThatThrownBy(() -> buzzThreadService.openThread(command(buzz, ThreadVisibility.PRIVATE)))
                .isInstanceOf(BuzzThreadAlreadyOpenedException.class);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT is_read FROM buzz_audience_members WHERE buzz_id = ? AND user_id = ?",
                Boolean.class, buzz.getId(), starterId)).isFalse();
    }

    @Test
    @DisplayName("스레드 열기가 버즈를 공유 락으로 읽은 뒤 삭제가 들어오면 삭제가 기다렸다가 새 스레드까지 지운다")
    void openLocksBuzzFirst_DeletionRemovesThread() throws InterruptedException {
        // given
        Buzz buzz = saveBuzz();
        CountDownLatch buzzLocked = new CountDownLatch(1);
        CountDownLatch continueOpen = new CountDownLatch(1);
        CountDownLatch openFinished = new CountDownLatch(1);
        CountDownLatch deletionFinished = new CountDownLatch(1);
        AtomicReference<Throwable> openFailure = new AtomicReference<>();
        AtomicReference<Throwable> deletionFailure = new AtomicReference<>();
        AtomicBoolean blockOnce = new AtomicBoolean(true);
        doAnswer(invocation -> {
            Object locked = invocation.callRealMethod();
            if (blockOnce.compareAndSet(true, false)) {
                buzzLocked.countDown();
                await(continueOpen, "스레드 열기 재개");
            }
            return locked;
        }).when(buzzRepositoryAdapter).findByIdForShare(buzz.getId());

        ExecutorService executor = Executors.newFixedThreadPool(2);
        submit(executor, () -> buzzThreadService.openThread(command(buzz, ThreadVisibility.PUBLIC)), openFailure, openFinished);

        try {
            // when
            assertThat(buzzLocked.await(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();
            submit(executor, () -> buzzService.deleteBuzz(creatorId, buzz.getId()), deletionFailure, deletionFinished);
            awaitFinishedOrLockWait(deletionFinished, "버즈 삭제");
            assertThat(deletionFinished.getCount()).isEqualTo(1);
            continueOpen.countDown();
            assertThat(openFinished.await(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();
            assertThat(deletionFinished.await(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();

            // then
            assertThat(openFailure.get()).isNull();
            assertThat(deletionFailure.get()).isNull();
            assertThat(countRows("buzz_threads", "buzz_id", buzz.getId())).isZero();
        } finally {
            continueOpen.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("삭제가 버즈 행을 먼저 지우면 스레드 열기는 기다렸다가 버즈를 찾지 못하고 스레드를 남기지 않는다")
    void deletionRemovesBuzzFirst_OpenFailsWithNotFound() throws InterruptedException {
        // given
        Buzz buzz = saveBuzz();
        CountDownLatch buzzDeleted = new CountDownLatch(1);
        CountDownLatch continueDeletion = new CountDownLatch(1);
        CountDownLatch deletionFinished = new CountDownLatch(1);
        CountDownLatch openFinished = new CountDownLatch(1);
        AtomicReference<Throwable> deletionFailure = new AtomicReference<>();
        AtomicReference<Throwable> openFailure = new AtomicReference<>();
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
            submit(executor, () -> buzzThreadService.openThread(command(buzz, ThreadVisibility.PUBLIC)), openFailure, openFinished);
            awaitFinishedOrLockWait(openFinished, "스레드 열기");
            continueDeletion.countDown();
            assertThat(deletionFinished.await(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();
            assertThat(openFinished.await(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();

            // then
            assertThat(deletionFailure.get()).isNull();
            assertThat(openFailure.get()).isInstanceOf(BuzzNotFoundException.class);
            assertThat(countRows("buzz_threads", "buzz_id", buzz.getId())).isZero();
        } finally {
            continueDeletion.countDown();
            executor.shutdownNow();
        }
    }

    private Buzz saveBuzz() {
        Buzz buzz = Buzz.create(creatorId, "본문", List.of(), List.of(starterId), LocalDateTime.now());
        buzzRepositoryAdapter.save(buzz);
        buzzAudienceMemberRepositoryAdapter.saveAll(List.of(BuzzAudienceMember.create(buzz.getId(), starterId)));
        createdBuzzIds.add(buzz.getId());
        return buzz;
    }

    private OpenBuzzThreadCommand command(Buzz buzz, ThreadVisibility visibility) {
        return new OpenBuzzThreadCommand(starterId, buzz.getId(), visibility, "첫 글", List.of());
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
