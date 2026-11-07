package com.example.DunbarHorizon.flag.domain.invitation;

import com.example.DunbarHorizon.flag.adapter.out.persistence.jpa.FlagInvitationJpaRepository;
import com.example.DunbarHorizon.flag.adapter.out.persistence.jpa.FlagJpaRepository;
import com.example.DunbarHorizon.flag.adapter.out.user.FlagUserAdapter;
import com.example.DunbarHorizon.flag.application.port.in.FlagHostUseCase;
import com.example.DunbarHorizon.flag.application.port.in.FlagInvitationUseCase;
import com.example.DunbarHorizon.flag.application.port.in.command.FlagHostCommand;
import com.example.DunbarHorizon.flag.domain.invitation.exception.FlagInvitationDuplicateException;
import com.example.DunbarHorizon.flag.domain.invitation.repository.FlagInvitationRepository;
import com.example.DunbarHorizon.support.TestContainerConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;

/**
 * 두 트랜잭션의 실제 커밋과 유니크 제약 대기를 재현해야 하므로 SpringBootTest로 실행하고
 * 생성 데이터를 직접 정리한다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestContainerConfig.class)
class FlagInvitationDuplicateConcurrencyIntegrationTest {

    private static final Long HOST_ID = 12101L;
    private static final Long INVITEE_ID = 12102L;
    private static final long WAIT_SECONDS = 10;

    @Autowired private FlagHostUseCase flagHostUseCase;
    @Autowired private FlagInvitationUseCase flagInvitationUseCase;
    @Autowired private FlagJpaRepository flagJpaRepository;
    @Autowired private FlagInvitationJpaRepository invitationJpaRepository;
    @Autowired private PlatformTransactionManager transactionManager;

    @MockitoSpyBean private FlagInvitationRepository invitationRepository;
    @MockitoBean private FlagUserAdapter flagUserAdapter;

    private final List<Long> createdFlagIds = new ArrayList<>();

    @AfterEach
    void tearDown() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            if (createdFlagIds.isEmpty()) {
                return;
            }
            invitationJpaRepository.hardDeleteByFlagIdsIn(createdFlagIds);
            flagJpaRepository.hardDeleteByIdsIn(createdFlagIds);
        });
        createdFlagIds.clear();
    }

    @Test
    @DisplayName("같은 사람을 동시에 초대하면 초대는 하나만 남고 나머지는 중복 예외를 받는다")
    void invite_Concurrent_LeavesSingleInvitation() throws InterruptedException {
        // given
        Long flagId = hostRecruitingFlag();
        CountDownLatch bothChecked = new CountDownLatch(2);
        CountDownLatch completed = new CountDownLatch(2);
        AtomicInteger successCount = new AtomicInteger();
        AtomicReference<Throwable> firstFailure = new AtomicReference<>();

        // 두 요청이 모두 사전 확인을 통과한 뒤에야 저장으로 넘어가게 한다.
        doAnswer(invocation -> {
            Object exists = invocation.callRealMethod();
            bothChecked.countDown();
            assertThat(bothChecked.await(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();
            return exists;
        }).when(invitationRepository).existsByFlagIdAndInviteeId(flagId, INVITEE_ID);

        // when
        ExecutorService executor = Executors.newFixedThreadPool(2);
        for (int i = 0; i < 2; i++) {
            executor.submit(() -> {
                try {
                    flagInvitationUseCase.invite(flagId, HOST_ID, INVITEE_ID);
                    successCount.incrementAndGet();
                } catch (Throwable throwable) {
                    firstFailure.compareAndSet(null, throwable);
                } finally {
                    completed.countDown();
                }
            });
        }
        assertThat(completed.await(WAIT_SECONDS * 2, TimeUnit.SECONDS)).isTrue();
        executor.shutdownNow();

        // then
        assertThat(invitationCount(flagId)).isEqualTo(1);
        assertThat(successCount).hasValue(1);
        assertThat(firstFailure.get()).isInstanceOf(FlagInvitationDuplicateException.class);
    }

    private long invitationCount(Long flagId) {
        return invitationJpaRepository.findAll().stream()
                .filter(invitation -> invitation.getFlagId().equals(flagId))
                .count();
    }

    private Long hostRecruitingFlag() {
        LocalDateTime base = LocalDateTime.now().withNano(0);
        Long flagId = flagHostUseCase.hostFlag(new FlagHostCommand(
                HOST_ID, "중복 초대 테스트", "설명", 5,
                base.plusHours(1), base.plusHours(2), base.plusHours(3)));
        createdFlagIds.add(flagId);
        return flagId;
    }
}
