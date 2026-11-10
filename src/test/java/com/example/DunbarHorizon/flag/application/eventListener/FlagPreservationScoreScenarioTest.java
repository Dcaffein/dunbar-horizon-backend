package com.example.DunbarHorizon.flag.application.eventListener;

import com.example.DunbarHorizon.flag.application.port.out.FlagMaintenancePort;
import com.example.DunbarHorizon.flag.application.service.flag.FlagExpiryService;
import com.example.DunbarHorizon.flag.domain.flag.Flag;
import com.example.DunbarHorizon.flag.domain.flag.FlagConclusionDecider;
import com.example.DunbarHorizon.flag.domain.flag.FlagExpiryExemptionUpdater;
import com.example.DunbarHorizon.flag.domain.flag.FlagSchedule;
import com.example.DunbarHorizon.flag.domain.flag.event.FlagConcludedEvent;
import com.example.DunbarHorizon.flag.domain.flag.event.FlagExpiryExemptionChangedEvent;
import com.example.DunbarHorizon.flag.domain.flag.repository.FlagRepository;
import com.example.DunbarHorizon.flag.domain.memorial.repository.FlagMemorialRepository;
import com.example.DunbarHorizon.global.event.interaction.BatchMutualInteractionEvent;
import com.example.DunbarHorizon.global.event.interaction.InteractionType;
import com.example.DunbarHorizon.social.domain.friend.InteractionScorePolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.mock;

/**
 * 면제 전이와 스윕을 실제 객체로 이어 붙여 플래그 하나에 들어간 순 점수를 본다.
 */
class FlagPreservationScoreScenarioTest {

    private static final Long FLAG_ID = 70L;
    private static final Long HOST_ID = 10L;
    private static final List<Long> PARTICIPANT_IDS = List.of(21L, 22L);

    private static final double BASE = InteractionScorePolicy.scoreOf(InteractionType.FLAG_ENDED);
    private static final double PRESERVED = InteractionScorePolicy.scoreOf(InteractionType.FLAG_PRESERVED);

    private final FlagRepository flagRepository = mock(FlagRepository.class);
    private final FlagMemorialRepository memorialRepository = mock(FlagMemorialRepository.class);
    private final FlagMaintenancePort maintenancePort = mock(FlagMaintenancePort.class);

    private final List<BatchMutualInteractionEvent> published = new ArrayList<>();
    private final ApplicationEventPublisher publisher = new RoutingPublisher();

    private final FlagExpiryExemptionUpdater exemptionUpdater =
            new FlagExpiryExemptionUpdater(flagRepository, memorialRepository);
    private final FlagExpiryExemptionEventListener exemptionListener =
            new FlagExpiryExemptionEventListener(flagRepository, publisher);
    private final FlagConclusionEventListener conclusionListener =
            new FlagConclusionEventListener(publisher);
    private final FlagExpiryService expiryService =
            new FlagExpiryService(flagRepository,
                    new FlagConclusionDecider(flagRepository, memorialRepository),
                    maintenancePort, publisher);

    private Flag flag;

    private class RoutingPublisher implements ApplicationEventPublisher {
        @Override
        public void publishEvent(Object event) {
            if (event instanceof FlagExpiryExemptionChangedEvent changed) {
                exemptionListener.handleExpiryExemptionChanged(changed);
            } else if (event instanceof FlagConcludedEvent concluded) {
                conclusionListener.handleFlagConclusion(concluded);
            } else if (event instanceof BatchMutualInteractionEvent score) {
                published.add(score);
            }
        }
    }

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();
        flag = Flag.create(HOST_ID, "종료된 모임", "설명", 10,
                FlagSchedule.of(now.minusHours(30), now.minusHours(29), now.minusHours(26)));
        ReflectionTestUtils.setField(flag, "id", FLAG_ID);

        given(flagRepository.findAllParticipantIds(FLAG_ID)).willReturn(PARTICIPANT_IDS);
        given(flagRepository.existsByParentId(FLAG_ID)).willReturn(false);

        // 면제가 걸린 플래그는 조회 조건에서 빠진다. 고정값으로 두면 스윕 시나리오가 거짓이 된다.
        willAnswer(invocation -> flag.isConcluded() ? List.of() : List.of(flag))
                .given(flagRepository).findConclusionCandidates(any(LocalDateTime.class));
        given(flagRepository.findAllParticipantIdsByFlagIds(any(Collection.class)))
                .willReturn(Map.of(FLAG_ID, PARTICIPANT_IDS));
    }

    @Test
    @DisplayName("후기도 앵코르도 없이 끝난 모임은 스윕에 집힐 때 기본 점수를 한 번분 받는다")
    void sweptFlag_receivesBaseScoreOnce() {
        // when
        sweep();

        // then
        assertThat(netScore()).isEqualTo(BASE);
    }

    @Test
    @DisplayName("후기가 달린 모임은 기본과 보존 가중치를 한 번분 받고 스윕에는 집히지 않는다")
    void preservedFlag_receivesPreservedScoreOnce() {
        // when
        writeMemorial();
        sweep();

        // then
        assertThat(netScore()).isEqualTo(PRESERVED);
        assertThat(flag.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("후기 작성 후 삭제하고 스윕에 집히면 기본 한 번분만 남는다")
    void memorialThenDeleteThenSweep_leavesBaseOnce() {
        // when
        writeMemorial();
        removeMemorial();
        sweep();

        // then
        assertThat(netScore()).isEqualTo(BASE);
    }

    @Test
    @DisplayName("후기를 지웠다 다시 쓰면 기본과 보존 가중치가 한 번분이다")
    void memorialThenDeleteThenRewrite_leavesPreservedOnce() {
        // when
        writeMemorial();
        removeMemorial();
        writeMemorial();

        // then
        assertThat(netScore()).isEqualTo(PRESERVED);
    }

    @Test
    @DisplayName("이미 면제가 꺼진 플래그를 다시 꺼도 아무 일이 일어나지 않는다")
    void secondRelease_changesNothing() {
        // when
        writeMemorial();
        removeMemorial();
        removeMemorial();

        // then
        assertThat(netScore()).isZero();
        assertThat(published).hasSize(2);
    }

    @Test
    @DisplayName("호스트가 보존된 모임을 삭제하면 받은 점수가 회수되어 합이 0이 된다")
    void hostDeletesPreservedFlag_netScoreIsZero() {
        // when
        writeMemorial();
        hostDeletes();

        // then
        assertThat(netScore()).isZero();
        assertThat(published).hasSize(2);
        assertThat(flag.isAutoExpiryExempt()).isFalse();
    }

    @Test
    @DisplayName("호스트가 보존되지 않은 모임을 삭제하면 점수가 발행되지 않는다")
    void hostDeletesPlainFlag_publishesNothing() {
        // when
        hostDeletes();

        // then
        assertThat(published).isEmpty();
    }

    private void writeMemorial() {
        given(memorialRepository.existsByFlagId(FLAG_ID)).willReturn(true);
        exemptionUpdater.refresh(flag);
        flushDomainEvents(flag);
    }

    private void removeMemorial() {
        given(memorialRepository.existsByFlagId(FLAG_ID)).willReturn(false);
        exemptionUpdater.refresh(flag);
        flushDomainEvents(flag);
    }

    private void hostDeletes() {
        flag.delete(HOST_ID);
        flushDomainEvents(flag);
    }

    private void sweep() {
        expiryService.concludeEndedFlags();
    }

    private void flushDomainEvents(Flag target) {
        List.copyOf(target.getDomainEvents()).forEach(publisher::publishEvent);
        ReflectionTestUtils.invokeMethod(target, "clearDomainEvents");
    }

    private double netScore() {
        return published.stream()
                .mapToDouble(event -> event.direction().signed(InteractionScorePolicy.scoreOf(event.type())))
                .sum();
    }
}
