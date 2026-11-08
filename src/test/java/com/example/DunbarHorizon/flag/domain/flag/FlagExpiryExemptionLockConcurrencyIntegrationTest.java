package com.example.DunbarHorizon.flag.domain.flag;

import com.example.DunbarHorizon.flag.adapter.out.persistence.jpa.FlagJpaRepository;
import com.example.DunbarHorizon.flag.adapter.out.persistence.jpa.FlagMemorialJpaRepository;
import com.example.DunbarHorizon.flag.application.port.in.FlagHostUseCase;
import com.example.DunbarHorizon.flag.application.port.in.FlagMemorialCommandUseCase;
import com.example.DunbarHorizon.flag.application.port.in.FlagModificationUseCase;
import com.example.DunbarHorizon.flag.application.port.in.command.FlagEncoreCommand;
import com.example.DunbarHorizon.flag.application.port.in.command.FlagHostCommand;
import com.example.DunbarHorizon.flag.domain.flag.repository.FlagRepository;
import com.example.DunbarHorizon.flag.domain.memorial.FlagMemorial;
import com.example.DunbarHorizon.flag.domain.memorial.repository.FlagMemorialRepository;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestContainerConfig.class)
class FlagExpiryExemptionLockConcurrencyIntegrationTest {

    private static final Long HOST_ID = 12901L;
    private static final long WAIT_SECONDS = 5;

    @Autowired private FlagHostUseCase flagHostUseCase;
    @Autowired private FlagMemorialCommandUseCase memorialCommandUseCase;
    @Autowired private FlagModificationUseCase flagModificationUseCase;
    @Autowired private FlagJpaRepository flagJpaRepository;
    @Autowired private FlagMemorialJpaRepository memorialJpaRepository;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private JdbcTemplate jdbcTemplate;

    @MockitoSpyBean private FlagMemorialRepository memorialRepository;
    @MockitoSpyBean private FlagRepository flagRepository;

    private final List<Long> createdFlagIds = new ArrayList<>();

    @AfterEach
    void tearDown() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            if (createdFlagIds.isEmpty()) {
                return;
            }
            memorialJpaRepository.hardDeleteByFlagIdsIn(createdFlagIds);
            flagJpaRepository.hardDeleteByIdsIn(createdFlagIds);
        });
        createdFlagIds.clear();
    }

    @Test
    @DisplayName("같은 Flag에서 Memorial 작성과 삭제가 겹쳐도 Memorial이 남으면 면제가 켜져 있다")
    void memorialCreateAndDelete_overlapping_keepsExemptionWhileMemorialRemains() throws InterruptedException {
        // given
        Long flagId = hostEndedFlag();
        Long firstMemorialId = memorialCommandUseCase.createMemorial(flagId, HOST_ID, "첫 후기");
        assertThat(flagJpaRepository.findById(flagId).orElseThrow().isAutoExpiryExempt()).isTrue();

        CountDownLatch secondSaved = new CountDownLatch(1);
        CountDownLatch continueCreate = new CountDownLatch(1);
        CountDownLatch createFinished = new CountDownLatch(1);
        CountDownLatch deleteFinished = new CountDownLatch(1);
        AtomicBoolean blockOnce = new AtomicBoolean(true);
        AtomicReference<Throwable> createFailure = new AtomicReference<>();
        AtomicReference<Throwable> deleteFailure = new AtomicReference<>();

        doAnswer(invocation -> {
            Object saved = invocation.callRealMethod();
            if (blockOnce.compareAndSet(true, false)) {
                secondSaved.countDown();
                await(continueCreate, "작성 트랜잭션 재개");
            }
            return saved;
        }).when(memorialRepository).save(any(FlagMemorial.class));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        executor.submit(() -> {
            try {
                memorialCommandUseCase.createMemorial(flagId, HOST_ID, "두 번째 후기");
            } catch (Throwable throwable) {
                createFailure.set(throwable);
            } finally {
                createFinished.countDown();
            }
        });

        try {
            // when
            assertThat(secondSaved.await(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();
            executor.submit(() -> {
                try {
                    memorialCommandUseCase.deleteMemorial(flagId, firstMemorialId, HOST_ID);
                } catch (Throwable throwable) {
                    deleteFailure.set(throwable);
                } finally {
                    deleteFinished.countDown();
                }
            });
            awaitFinishedOrLockWait(deleteFinished, "삭제 트랜잭션");
            continueCreate.countDown();
            assertThat(createFinished.await(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();
            assertThat(deleteFinished.await(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();

            // then
            assertThat(createFailure.get()).isNull();
            assertThat(deleteFailure.get()).isNull();
            assertThat(memorialJpaRepository.countByFlagId(flagId)).isEqualTo(1);
            assertThat(flagJpaRepository.findById(flagId).orElseThrow().isAutoExpiryExempt()).isTrue();
        } finally {
            continueCreate.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("호스트 삭제가 Flag를 읽은 뒤 첫 Memorial이 끼어들어도 삭제는 최신 면제를 보고 끈다")
    void hostDeletion_withFirstMemorialInBetween_turnsOffLatestExemption() throws InterruptedException {
        // given
        Long flagId = hostEndedFlag();
        CountDownLatch hostRead = new CountDownLatch(1);
        CountDownLatch continueHost = new CountDownLatch(1);
        CountDownLatch hostFinished = new CountDownLatch(1);
        CountDownLatch memorialFinished = new CountDownLatch(1);
        AtomicBoolean blockOnce = new AtomicBoolean(true);
        AtomicReference<Throwable> hostFailure = new AtomicReference<>();
        AtomicReference<Throwable> memorialFailure = new AtomicReference<>();

        Answer<Object> pauseAfterFirstRead = invocation -> {
            Object result = invocation.callRealMethod();
            if (blockOnce.compareAndSet(true, false)) {
                hostRead.countDown();
                await(continueHost, "호스트 삭제 재개");
            }
            return result;
        };
        doAnswer(pauseAfterFirstRead).when(flagRepository).findById(flagId);
        doAnswer(pauseAfterFirstRead).when(flagRepository).findByIdForUpdate(flagId);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        submit(executor, () -> flagModificationUseCase.closeFlag(flagId, HOST_ID), hostFailure, hostFinished);

        try {
            // when
            assertThat(hostRead.await(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();
            submit(executor, () -> memorialCommandUseCase.createMemorial(flagId, HOST_ID, "첫 후기"),
                    memorialFailure, memorialFinished);
            awaitFinishedOrLockWait(memorialFinished, "Memorial 작성");
            continueHost.countDown();
            assertThat(hostFinished.await(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();
            assertThat(memorialFinished.await(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();

            // then
            assertThat(hostFailure.get()).isNull();
            assertThat(flagJpaRepository.findById(flagId)).isEmpty();
            assertThat(exemptOfIncludingDeleted(flagId)).isFalse();
        } finally {
            continueHost.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("Encore 생성과 부모의 Memorial 삭제가 겹쳐도 Encore가 있으면 부모 면제가 켜져 있다")
    void encoreCreationAndParentMemorialDeletion_overlapping_keepsParentExemption() throws InterruptedException {
        // given
        Long parentId = hostEndedFlag();
        Long memorialId = memorialCommandUseCase.createMemorial(parentId, HOST_ID, "부모 후기");
        CountDownLatch encoreSaved = new CountDownLatch(1);
        CountDownLatch continueEncore = new CountDownLatch(1);
        CountDownLatch encoreFinished = new CountDownLatch(1);
        CountDownLatch deleteFinished = new CountDownLatch(1);
        AtomicBoolean blockOnce = new AtomicBoolean(true);
        AtomicReference<Throwable> encoreFailure = new AtomicReference<>();
        AtomicReference<Throwable> deleteFailure = new AtomicReference<>();
        AtomicReference<Long> encoreId = new AtomicReference<>();

        doAnswer(invocation -> {
            Flag saved = (Flag) invocation.callRealMethod();
            if (saved.getParentId() != null && blockOnce.compareAndSet(true, false)) {
                encoreSaved.countDown();
                await(continueEncore, "Encore 생성 재개");
            }
            return saved;
        }).when(flagRepository).save(any(Flag.class));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        submit(executor, () -> encoreId.set(encore(parentId)), encoreFailure, encoreFinished);

        try {
            // when
            assertThat(encoreSaved.await(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();
            submit(executor, () -> memorialCommandUseCase.deleteMemorial(parentId, memorialId, HOST_ID),
                    deleteFailure, deleteFinished);
            awaitFinishedOrLockWait(deleteFinished, "부모 Memorial 삭제");
            continueEncore.countDown();
            assertThat(encoreFinished.await(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();
            assertThat(deleteFinished.await(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();

            // then
            assertThat(encoreFailure.get()).isNull();
            assertThat(deleteFailure.get()).isNull();
            assertThat(flagJpaRepository.findById(encoreId.get())).isPresent();
            assertThat(flagJpaRepository.findById(parentId).orElseThrow().isAutoExpiryExempt()).isTrue();
        } finally {
            continueEncore.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("Encore 삭제가 Encore를 잠근 사이 부모에 Memorial이 달려도 교착 없이 둘 다 끝난다")
    void encoreDeletionAndParentMemorialCreation_overlapping_finishWithoutDeadlock() throws InterruptedException {
        // given
        Long parentId = hostEndedFlag();
        Long encoreId = encore(parentId);
        CountDownLatch encoreLocked = new CountDownLatch(1);
        CountDownLatch continueDeletion = new CountDownLatch(1);
        CountDownLatch deletionFinished = new CountDownLatch(1);
        CountDownLatch memorialFinished = new CountDownLatch(1);
        AtomicBoolean blockOnce = new AtomicBoolean(true);
        AtomicReference<Throwable> deletionFailure = new AtomicReference<>();
        AtomicReference<Throwable> memorialFailure = new AtomicReference<>();

        doAnswer(invocation -> {
            Object locked = invocation.callRealMethod();
            if (blockOnce.compareAndSet(true, false)) {
                encoreLocked.countDown();
                await(continueDeletion, "Encore 삭제 재개");
            }
            return locked;
        }).when(flagRepository).findByIdForUpdate(encoreId);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        submit(executor, () -> flagModificationUseCase.closeFlag(encoreId, HOST_ID), deletionFailure, deletionFinished);

        try {
            // when
            assertThat(encoreLocked.await(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();
            submit(executor, () -> memorialCommandUseCase.createMemorial(parentId, HOST_ID, "부모 후기"),
                    memorialFailure, memorialFinished);
            awaitFinishedOrLockWait(memorialFinished, "부모 Memorial 작성");
            continueDeletion.countDown();
            assertThat(deletionFinished.await(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();
            assertThat(memorialFinished.await(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();

            // then
            assertThat(deletionFailure.get()).isNull();
            assertThat(memorialFailure.get()).isNull();
            assertThat(flagJpaRepository.findById(encoreId)).isEmpty();
            assertThat(flagJpaRepository.findById(parentId).orElseThrow().isAutoExpiryExempt()).isTrue();
        } finally {
            continueDeletion.countDown();
            executor.shutdownNow();
        }
    }

    private Long encore(Long parentId) {
        LocalDateTime base = LocalDateTime.now().withNano(0);
        Long encoreId = flagHostUseCase.encoreFlag(new FlagEncoreCommand(
                parentId, HOST_ID, base.plusHours(1), base.plusHours(2), base.plusHours(3)));
        createdFlagIds.add(encoreId);
        return encoreId;
    }

    private boolean exemptOfIncludingDeleted(Long flagId) {
        Boolean exempt = jdbcTemplate.queryForObject(
                "SELECT auto_expiry_exempt FROM flags WHERE id = ?", Boolean.class, flagId);
        return Boolean.TRUE.equals(exempt);
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

    private Long hostEndedFlag() {
        LocalDateTime base = LocalDateTime.now().withNano(0);
        Long flagId = flagHostUseCase.hostFlag(new FlagHostCommand(
                HOST_ID, "면제 잠금 테스트", "설명", 5,
                base.minusHours(4), base.minusHours(3), base.minusHours(1)));
        createdFlagIds.add(flagId);
        return flagId;
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
        return waiting != null && waiting > 0;
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
