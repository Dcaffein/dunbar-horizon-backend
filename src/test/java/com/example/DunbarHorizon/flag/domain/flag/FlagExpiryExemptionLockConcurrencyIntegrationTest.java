package com.example.DunbarHorizon.flag.domain.flag;

import com.example.DunbarHorizon.flag.adapter.out.persistence.jpa.FlagJpaRepository;
import com.example.DunbarHorizon.flag.adapter.out.persistence.jpa.FlagMemorialJpaRepository;
import com.example.DunbarHorizon.flag.application.port.in.FlagHostUseCase;
import com.example.DunbarHorizon.flag.application.port.in.FlagMemorialCommandUseCase;
import com.example.DunbarHorizon.flag.application.port.in.command.FlagHostCommand;
import com.example.DunbarHorizon.flag.domain.memorial.FlagMemorial;
import com.example.DunbarHorizon.flag.domain.memorial.repository.FlagMemorialRepository;
import com.example.DunbarHorizon.support.TestContainerConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
    @Autowired private FlagJpaRepository flagJpaRepository;
    @Autowired private FlagMemorialJpaRepository memorialJpaRepository;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private JdbcTemplate jdbcTemplate;

    @MockitoSpyBean private FlagMemorialRepository memorialRepository;

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
