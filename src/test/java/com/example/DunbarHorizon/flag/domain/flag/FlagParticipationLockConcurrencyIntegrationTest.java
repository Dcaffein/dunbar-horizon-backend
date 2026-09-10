package com.example.DunbarHorizon.flag.domain.flag;

import com.example.DunbarHorizon.flag.adapter.out.persistence.jpa.FlagJpaRepository;
import com.example.DunbarHorizon.flag.adapter.out.persistence.jpa.FlagInvitationJpaRepository;
import com.example.DunbarHorizon.flag.adapter.out.persistence.jpa.FlagParticipantJpaRepository;
import com.example.DunbarHorizon.flag.adapter.out.user.FlagUserAdapter;
import com.example.DunbarHorizon.flag.application.port.in.FlagHostUseCase;
import com.example.DunbarHorizon.flag.application.port.in.FlagInvitationUseCase;
import com.example.DunbarHorizon.flag.application.port.in.FlagModificationUseCase;
import com.example.DunbarHorizon.flag.application.port.in.FlagParticipationUseCase;
import com.example.DunbarHorizon.flag.application.port.in.command.FlagCapacityUpdateCommand;
import com.example.DunbarHorizon.flag.application.port.in.command.FlagHostCommand;
import com.example.DunbarHorizon.flag.application.port.in.command.FlagScheduleUpdateCommand;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagDeadlinePassedException;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagFullCapacityException;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagInvalidStatusException;
import com.example.DunbarHorizon.flag.domain.flag.repository.FlagRepository;
import com.example.DunbarHorizon.flag.domain.invitation.FlagInvitation;
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
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.ArgumentMatchers.anyLong;

/**
 * @JpaRepositoryTest는 클래스 전체를 한 트랜잭션으로 묶는다. 이 검증은 두 트랜잭션의 실제 커밋과
 * PESSIMISTIC_WRITE 대기를 재현해야 하므로 SpringBootTest로 실행하고 생성 데이터를 직접 정리한다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestContainerConfig.class)
class FlagParticipationLockConcurrencyIntegrationTest {

    private static final Long HOST_ID = 11401L;
    private static final Long PARTICIPANT_A_ID = 11402L;
    private static final Long PARTICIPANT_B_ID = 11403L;
    private static final long WAIT_SECONDS = 5;

    @Autowired private FlagHostUseCase flagHostUseCase;
    @Autowired private FlagModificationUseCase flagModificationUseCase;
    @Autowired private FlagParticipationUseCase flagParticipationUseCase;
    @Autowired private FlagInvitationUseCase flagInvitationUseCase;
    @Autowired private FlagParticipationManager flagParticipationManager;
    @Autowired private FlagJpaRepository flagJpaRepository;
    @Autowired private FlagInvitationJpaRepository invitationJpaRepository;
    @Autowired private FlagParticipantJpaRepository participantJpaRepository;
    @Autowired private PlatformTransactionManager transactionManager;

    @MockitoSpyBean private FlagRepository flagRepository;
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
            participantJpaRepository.hardDeleteByFlagIdsIn(createdFlagIds);
            flagJpaRepository.hardDeleteByIdsIn(createdFlagIds);
        });
        createdFlagIds.clear();
    }

    @Test
    @DisplayName("스칼라 선행 조회 뒤 락을 얻으면 마감된 최신 Flag 상태를 읽는다")
    void projectionBeforeLock_readsFreshFlagState() throws InterruptedException {
        // given
        Long flagId = hostRecruitingFlag(2);
        CountDownLatch projectionRead = new CountDownLatch(1);
        CountDownLatch continueToLock = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(1);
        AtomicReference<Boolean> recruitingAtLock = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                // 트랜잭션 A: 스칼라 일반 조회 뒤, 트랜잭션 B의 모집 마감 커밋을 기다린다.
                transactionTemplate.executeWithoutResult(status -> {
                    Optional<Long> hostId = flagRepository.findHostIdById(flagId);
                    assertThat(hostId).contains(HOST_ID);
                    projectionRead.countDown();
                    await(continueToLock, "락 조회 재개");
                    recruitingAtLock.set(flagRepository.findByIdForUpdate(flagId)
                            .orElseThrow()
                            .isRecruiting());
                });
            } catch (Throwable throwable) {
                failure.set(throwable);
            } finally {
                finished.countDown();
            }
        });

        // when
        assertThat(projectionRead.await(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();
        // 트랜잭션 B: A가 Flag 락을 잡기 전에 모집을 마감하고 커밋한다.
        flagModificationUseCase.closeRecruitment(flagId, HOST_ID);
        continueToLock.countDown();
        assertThat(finished.await(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();
        executor.shutdownNow();

        // then
        assertThat(failure.get()).isNull();
        assertThat(recruitingAtLock).hasValue(false);
    }

    @Test
    @DisplayName("일반 참여가 락 전 대기 중 모집이 마감되면 최신 상태로 거절된다")
    void participate_afterRecruitmentClosedBeforeLock_throwsDeadlinePassed() throws InterruptedException {
        // given
        Long flagId = hostRecruitingFlag(2);
        given(flagUserAdapter.areFriends(HOST_ID, PARTICIPANT_A_ID)).willReturn(true);
        CountDownLatch hostIdRead = new CountDownLatch(1);
        CountDownLatch continueToLock = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicBoolean blockOnce = new AtomicBoolean(true);

        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Optional<Long> hostId = (Optional<Long>) invocation.callRealMethod();
            if (blockOnce.compareAndSet(true, false)) {
                hostIdRead.countDown();
                await(continueToLock, "일반 참여 락 조회 재개");
            }
            return hostId;
        }).when(flagRepository).findHostIdById(flagId);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                flagParticipationUseCase.participateInFlag(flagId, PARTICIPANT_A_ID);
            } catch (Throwable throwable) {
                failure.set(throwable);
            }
        });

        // when
        assertThat(hostIdRead.await(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();
        flagModificationUseCase.closeRecruitment(flagId, HOST_ID);
        continueToLock.countDown();
        executor.shutdown();
        assertThat(executor.awaitTermination(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();

        // then
        assertThat(failure.get()).isInstanceOf(FlagDeadlinePassedException.class);
        assertThat(participantJpaRepository.countByFlagId(flagId)).isZero();
    }

    @Test
    @DisplayName("초대 참여가 락 전 대기 중 모집이 마감되면 최신 상태로 거절된다")
    void invitationParticipation_afterRecruitmentClosedBeforeLock_throwsDeadlinePassed() throws InterruptedException {
        // given
        Long flagId = hostRecruitingFlag(2);
        CountDownLatch lockAttempted = new CountDownLatch(1);
        CountDownLatch continueToLock = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicBoolean blockOnce = new AtomicBoolean(true);
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        doAnswer(invocation -> {
            if (blockOnce.compareAndSet(true, false)) {
                lockAttempted.countDown();
                await(continueToLock, "초대 참여 락 조회 재개");
            }
            return invocation.callRealMethod();
        }).when(flagRepository).findByIdForUpdate(flagId);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                transactionTemplate.executeWithoutResult(status ->
                        flagParticipationManager.participateByInvitation(flagId, PARTICIPANT_A_ID));
            } catch (Throwable throwable) {
                failure.set(throwable);
            }
        });

        // when
        assertThat(lockAttempted.await(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();
        flagModificationUseCase.closeRecruitment(flagId, HOST_ID);
        continueToLock.countDown();
        executor.shutdown();
        assertThat(executor.awaitTermination(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();

        // then
        assertThat(failure.get()).isInstanceOf(FlagDeadlinePassedException.class);
    }

    @Test
    @DisplayName("일정 변경은 모집 마감 커밋 뒤 Flag 락에서 최신 상태를 읽는다")
    void reschedule_afterRecruitmentClosedBeforeLock_readsLatestFlagState() throws InterruptedException {
        // given
        Long flagId = hostRecruitingFlag(2);
        LocalDateTime base = LocalDateTime.now().withNano(0);
        FlagScheduleUpdateCommand command = new FlagScheduleUpdateCommand(
                flagId, HOST_ID, base.plusHours(1), base.plusHours(2), base.plusHours(3));
        CountDownLatch lockAttempted = new CountDownLatch(1);
        CountDownLatch continueToLock = new CountDownLatch(1);
        CountDownLatch completed = new CountDownLatch(1);
        AtomicBoolean blockOnce = new AtomicBoolean(true);
        AtomicReference<Boolean> recruitingAtLock = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();

        doAnswer(invocation -> {
            if (blockOnce.compareAndSet(true, false)) {
                lockAttempted.countDown();
                await(continueToLock, "일정 변경 락 조회 재개");
                @SuppressWarnings("unchecked")
                Optional<Flag> lockedFlag = (Optional<Flag>) invocation.callRealMethod();
                recruitingAtLock.set(lockedFlag.orElseThrow().isRecruiting());
                return lockedFlag;
            }
            return invocation.callRealMethod();
        }).when(flagRepository).findByIdForUpdate(flagId);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                flagModificationUseCase.reschedule(command);
            } catch (Throwable throwable) {
                failure.set(throwable);
            } finally {
                completed.countDown();
            }
        });

        try {
            // when
            assertThat(lockAttempted.await(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();
            flagModificationUseCase.closeRecruitment(flagId, HOST_ID);
            continueToLock.countDown();
            assertThat(completed.await(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();

            // then
            assertThat(failure.get()).isNull();
            assertThat(recruitingAtLock).hasValue(false);
            // 현행 정책: 마감 뒤에도 미래 deadline으로 재일정하면 모집이 다시 열린다.
            assertThat(flagJpaRepository.findById(flagId).orElseThrow().isRecruiting()).isTrue();
        } finally {
            continueToLock.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("정원 변경은 락 전 일반 조회가 있어도 커밋된 참여자 수보다 작게 줄일 수 없다")
    void modifyCapacity_afterProjectionBeforeLock_rejectsBelowCommittedParticipantCount() throws InterruptedException {
        // given
        Long flagId = hostRecruitingFlag(3);
        given(flagUserAdapter.areFriends(HOST_ID, PARTICIPANT_A_ID)).willReturn(true);
        given(flagUserAdapter.areFriends(HOST_ID, PARTICIPANT_B_ID)).willReturn(true);
        flagParticipationUseCase.participateInFlag(flagId, PARTICIPANT_A_ID);

        CountDownLatch projectionRead = new CountDownLatch(1);
        CountDownLatch continueToLock = new CountDownLatch(1);
        CountDownLatch completed = new CountDownLatch(1);
        AtomicBoolean blockOnce = new AtomicBoolean(true);
        AtomicReference<Throwable> failure = new AtomicReference<>();

        doAnswer(invocation -> {
            if (blockOnce.compareAndSet(true, false)) {
                // 트랜잭션 A: 테스트용 일반 조회가 REPEATABLE READ 읽기 뷰를 먼저 만든다.
                assertThat(flagRepository.findHostIdById(flagId)).contains(HOST_ID);
                projectionRead.countDown();
                await(continueToLock, "정원 변경 락 조회 재개");
            }
            return invocation.callRealMethod();
        }).when(flagRepository).findByIdForUpdate(flagId);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                flagModificationUseCase.modifyFlagCapacity(new FlagCapacityUpdateCommand(flagId, HOST_ID, 1));
            } catch (Throwable throwable) {
                failure.set(throwable);
            } finally {
                completed.countDown();
            }
        });

        try {
            // when
            assertThat(projectionRead.await(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();
            // 트랜잭션 B: A가 Flag 락을 얻기 전 두 번째 참여를 커밋한다.
            flagParticipationUseCase.participateInFlag(flagId, PARTICIPANT_B_ID);
            continueToLock.countDown();
            assertThat(completed.await(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();

            // then
            assertThat(failure.get()).isInstanceOf(FlagInvalidStatusException.class);
            assertThat(participantJpaRepository.countByFlagId(flagId)).isEqualTo(2);
            assertThat(flagJpaRepository.findById(flagId).orElseThrow().getCapacity()).isEqualTo(3);
        } finally {
            continueToLock.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("정원이 한 자리 남았을 때 두 참여 요청 중 하나만 성공한다")
    void concurrentParticipation_withOneRemainingSeat_preservesCapacity() throws InterruptedException {
        // given
        Long flagId = hostRecruitingFlag(1);
        given(flagUserAdapter.areFriends(HOST_ID, PARTICIPANT_A_ID)).willReturn(true);
        given(flagUserAdapter.areFriends(HOST_ID, PARTICIPANT_B_ID)).willReturn(true);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch completed = new CountDownLatch(2);
        AtomicInteger successCount = new AtomicInteger();
        AtomicReference<Throwable> firstFailure = new AtomicReference<>();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        submitParticipation(executor, start, completed, successCount, firstFailure, flagId, PARTICIPANT_A_ID);
        submitParticipation(executor, start, completed, successCount, firstFailure, flagId, PARTICIPANT_B_ID);

        // when
        start.countDown();
        assertThat(completed.await(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();
        assertThat(executor.awaitTermination(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();

        // then
        assertThat(successCount).hasValue(1);
        assertThat(firstFailure.get()).isInstanceOf(FlagFullCapacityException.class);
        assertThat(participantJpaRepository.countByFlagId(flagId)).isLessThanOrEqualTo(1);
    }

    @Test
    @DisplayName("초대 참여는 정원이 한 자리 남았을 때 두 요청 중 하나만 성공한다")
    void concurrentInvitationParticipation_withOneRemainingSeat_preservesCapacity() throws InterruptedException {
        // given
        Long flagId = hostRecruitingFlag(1);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch completed = new CountDownLatch(2);
        AtomicInteger successCount = new AtomicInteger();
        AtomicReference<Throwable> firstFailure = new AtomicReference<>();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        submitInvitationParticipation(executor, start, completed, successCount, firstFailure,
                flagId, PARTICIPANT_A_ID);
        submitInvitationParticipation(executor, start, completed, successCount, firstFailure,
                flagId, PARTICIPANT_B_ID);

        // when
        start.countDown();
        assertThat(completed.await(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();
        assertThat(executor.awaitTermination(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();

        // then
        assertThat(successCount).hasValue(1);
        assertThat(firstFailure.get()).isInstanceOf(FlagFullCapacityException.class);
        assertThat(participantJpaRepository.countByFlagId(flagId)).isLessThanOrEqualTo(1);
    }

    @Test
    @DisplayName("초대 수락은 정원이 한 자리 남았을 때 두 요청 중 하나만 성공한다")
    void concurrentInvitationAcceptance_withOneRemainingSeat_preservesCapacity() throws InterruptedException {
        // given
        Long flagId = hostRecruitingFlag(1);
        Long invitationAId = invitationRepository.save(FlagInvitation.create(flagId, HOST_ID, PARTICIPANT_A_ID)).getId();
        Long invitationBId = invitationRepository.save(FlagInvitation.create(flagId, HOST_ID, PARTICIPANT_B_ID)).getId();
        CountDownLatch invitationsRead = new CountDownLatch(2);
        CountDownLatch continueToParticipation = new CountDownLatch(1);
        CountDownLatch completed = new CountDownLatch(2);
        AtomicInteger successCount = new AtomicInteger();
        AtomicReference<Throwable> firstFailure = new AtomicReference<>();

        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Optional<FlagInvitation> invitation = (Optional<FlagInvitation>) invocation.callRealMethod();
            Long invitationId = invocation.getArgument(0);
            if (invitationId.equals(invitationAId) || invitationId.equals(invitationBId)) {
                invitationsRead.countDown();
                await(continueToParticipation, "초대 수락 참여 확정 재개");
            }
            return invitation;
        }).when(invitationRepository).findById(anyLong());

        ExecutorService executor = Executors.newFixedThreadPool(2);
        submitInvitationAcceptance(executor, completed, successCount, firstFailure, invitationAId, PARTICIPANT_A_ID);
        submitInvitationAcceptance(executor, completed, successCount, firstFailure, invitationBId, PARTICIPANT_B_ID);

        // when
        assertThat(invitationsRead.await(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();
        continueToParticipation.countDown();
        assertThat(completed.await(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();
        assertThat(executor.awaitTermination(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();

        // then
        assertThat(successCount).hasValue(1);
        assertThat(firstFailure.get()).isInstanceOf(FlagFullCapacityException.class);
        assertThat(participantJpaRepository.countByFlagId(flagId)).isEqualTo(1);
    }

    private Long hostRecruitingFlag(int capacity) {
        LocalDateTime base = LocalDateTime.now().withNano(0);
        Long flagId = flagHostUseCase.hostFlag(new FlagHostCommand(
                HOST_ID, "락 테스트", "설명", capacity,
                base.plusHours(1), base.plusHours(2), base.plusHours(3)));
        createdFlagIds.add(flagId);
        return flagId;
    }

    private void submitParticipation(ExecutorService executor, CountDownLatch start, CountDownLatch completed,
                                     AtomicInteger successCount, AtomicReference<Throwable> firstFailure,
                                     Long flagId, Long participantId) {
        executor.submit(() -> {
            try {
                await(start, "동시 참여 시작");
                flagParticipationUseCase.participateInFlag(flagId, participantId);
                successCount.incrementAndGet();
            } catch (Throwable throwable) {
                firstFailure.compareAndSet(null, throwable);
            } finally {
                completed.countDown();
            }
        });
    }

    private void submitInvitationParticipation(ExecutorService executor, CountDownLatch start,
                                               CountDownLatch completed, AtomicInteger successCount,
                                               AtomicReference<Throwable> firstFailure,
                                               Long flagId, Long participantId) {
        executor.submit(() -> {
            try {
                await(start, "동시 초대 참여 시작");
                new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                    FlagParticipant participant = flagParticipationManager.participateByInvitation(flagId, participantId);
                    flagRepository.saveParticipant(participant);
                });
                successCount.incrementAndGet();
            } catch (Throwable throwable) {
                firstFailure.compareAndSet(null, throwable);
            } finally {
                completed.countDown();
            }
        });
    }

    private void submitInvitationAcceptance(ExecutorService executor, CountDownLatch completed,
                                            AtomicInteger successCount, AtomicReference<Throwable> firstFailure,
                                            Long invitationId, Long inviteeId) {
        executor.submit(() -> {
            try {
                flagInvitationUseCase.accept(invitationId, inviteeId);
                successCount.incrementAndGet();
            } catch (Throwable throwable) {
                firstFailure.compareAndSet(null, throwable);
            } finally {
                completed.countDown();
            }
        });
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
