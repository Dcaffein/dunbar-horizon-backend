package com.example.DunbarHorizon.flag.application.eventListener;

import com.example.DunbarHorizon.flag.application.port.out.FlagMaintenancePort;
import com.example.DunbarHorizon.flag.application.service.flag.FlagExpiryService;
import com.example.DunbarHorizon.flag.domain.flag.Flag;
import com.example.DunbarHorizon.flag.domain.flag.FlagConclusionDecider;
import com.example.DunbarHorizon.flag.domain.flag.FlagSchedule;
import com.example.DunbarHorizon.flag.domain.flag.event.FlagConcludedEvent;
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
 * 스윕과 결말 리스너를 실제 객체로 이어 붙여 플래그 하나에 들어간 순 점수를 본다.
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

    private final FlagConclusionEventListener conclusionListener =
            new FlagConclusionEventListener(publisher);
    private final FlagExpiryService expiryService = new FlagExpiryService(
            flagRepository, new FlagConclusionDecider(flagRepository, memorialRepository),
            maintenancePort, publisher);

    private Flag flag;

    private class RoutingPublisher implements ApplicationEventPublisher {
        @Override
        public void publishEvent(Object event) {
            if (event instanceof FlagConcludedEvent concluded) {
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

        given(flagRepository.existsByParentId(FLAG_ID)).willReturn(false);
        given(memorialRepository.existsByFlagId(FLAG_ID)).willReturn(false);

        // 결말이 난 플래그는 조회 조건에서 빠진다. 고정값으로 두면 두 번째 스윕 시나리오가 거짓이 된다.
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
        sweep();

        // then
        assertThat(netScore()).isEqualTo(BASE);
        assertThat(flag.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("후기가 달린 모임은 스윕에서 보존되어 보존 점수를 한 번분 받는다")
    void flagWithMemorial_receivesPreservedScoreOnce() {
        // given
        given(memorialRepository.existsByFlagId(FLAG_ID)).willReturn(true);

        // when
        sweep();
        sweep();

        // then
        assertThat(netScore()).isEqualTo(PRESERVED);
        assertThat(flag.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("보존된 뒤 후기가 모두 지워져도 다음 스윕은 결말과 점수를 바꾸지 않는다")
    void preservedFlag_staysPreserved_afterMemorialsRemoved() {
        // given
        given(memorialRepository.existsByFlagId(FLAG_ID)).willReturn(true);
        sweep();

        // when
        given(memorialRepository.existsByFlagId(FLAG_ID)).willReturn(false);
        sweep();

        // then
        assertThat(netScore()).isEqualTo(PRESERVED);
        assertThat(published).hasSize(1);
        assertThat(flag.isDeleted()).isFalse();
        assertThat(flag.isPreserved()).isTrue();
    }

    @Test
    @DisplayName("호스트가 보존된 모임을 삭제해도 받은 점수는 회수되지 않는다")
    void hostDeletesPreservedFlag_keepsScore() {
        // given
        given(memorialRepository.existsByFlagId(FLAG_ID)).willReturn(true);
        sweep();

        // when
        flag.delete(HOST_ID);

        // then
        assertThat(netScore()).isEqualTo(PRESERVED);
        assertThat(published).hasSize(1);
    }

    @Test
    @DisplayName("결말 전에 호스트가 삭제한 모임은 스윕에 집히지 않아 점수가 발행되지 않는다")
    void hostDeletesBeforeConclusion_publishesNothing() {
        // given
        flag.delete(HOST_ID);

        // when
        sweep();

        // then
        assertThat(published).isEmpty();
    }

    private void sweep() {
        expiryService.concludeEndedFlags();
    }

    private double netScore() {
        return published.stream()
                .mapToDouble(event -> event.direction().signed(InteractionScorePolicy.scoreOf(event.type())))
                .sum();
    }
}
