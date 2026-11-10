package com.example.DunbarHorizon.flag.application.service.flag;

import com.example.DunbarHorizon.flag.domain.flag.Flag;
import com.example.DunbarHorizon.flag.domain.flag.FlagConclusionDecider;
import com.example.DunbarHorizon.flag.domain.flag.FlagSchedule;
import com.example.DunbarHorizon.flag.domain.flag.event.FlagConcludedEvent;
import com.example.DunbarHorizon.flag.domain.flag.event.FlagConclusionKind;
import com.example.DunbarHorizon.flag.domain.flag.repository.FlagRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FlagExpiryServiceTest {

    @InjectMocks private FlagExpiryService flagExpiryService;
    @Mock private FlagRepository flagRepository;
    @Mock private FlagConclusionDecider conclusionDecider;
    @Mock private ApplicationEventPublisher eventPublisher;

    private static Flag endedFlag(Long id, Long hostId, Long parentId) {
        LocalDateTime now = LocalDateTime.now();
        Flag flag = Flag.create(hostId, "종료된 모임", "설명", 10,
                FlagSchedule.of(now.minusHours(30), now.minusHours(29), now.minusHours(26)));
        ReflectionTestUtils.setField(flag, "id", id);
        ReflectionTestUtils.setField(flag, "parentId", parentId);
        return flag;
    }

    private void givenCandidates(List<Flag> candidates, Map<Long, List<Long>> participants) {
        given(flagRepository.findConclusionCandidates(any())).willReturn(candidates);
        given(flagRepository.findAllParticipantIdsByFlagIds(any())).willReturn(participants);
        given(conclusionDecider.conclude(any(), any())).willReturn(FlagConclusionKind.ENDED);
    }

    private List<FlagConcludedEvent> publishedConclusions() {
        ArgumentCaptor<FlagConcludedEvent> captor = ArgumentCaptor.forClass(FlagConcludedEvent.class);
        verify(eventPublisher, atLeastOnce()).publishEvent(captor.capture());
        return captor.getAllValues();
    }

    @Test
    @DisplayName("만료 임계값이 현재 시각 기준 24시간 전으로 계산된다")
    void concludeEndedFlags_ThresholdIs24HoursBefore() {
        // given
        given(flagRepository.findConclusionCandidates(any())).willReturn(List.of());
        LocalDateTime before = LocalDateTime.now().minusHours(Flag.EXPIRATION_THRESHOLD_HOURS);

        // when
        flagExpiryService.concludeEndedFlags();

        // then
        ArgumentCaptor<LocalDateTime> thresholdCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(flagRepository).findConclusionCandidates(thresholdCaptor.capture());
        assertThat(thresholdCaptor.getValue()).isBetween(before.minusSeconds(1), before.plusSeconds(1));
    }

    @Test
    @DisplayName("후보마다 결말을 정하고 저장한다")
    void concludeEndedFlags_ConcludesAndSavesEachCandidate() {
        // given
        Flag first = endedFlag(1L, 10L, null);
        Flag second = endedFlag(2L, 11L, null);
        givenCandidates(List.of(first, second), Map.of(1L, List.of(21L), 2L, List.of(22L)));

        // when
        flagExpiryService.concludeEndedFlags();

        // then
        InOrder inOrder = inOrder(conclusionDecider, flagRepository);
        inOrder.verify(conclusionDecider).conclude(eq(first), any(LocalDateTime.class));
        inOrder.verify(flagRepository).save(first);
        inOrder.verify(conclusionDecider).conclude(eq(second), any(LocalDateTime.class));
        inOrder.verify(flagRepository).save(second);
    }

    @Test
    @DisplayName("결말 종류대로 삭제면 ENDED, 보존이면 PRESERVED를 발행한다")
    void concludeEndedFlags_PublishesDecidedKind() {
        // given
        Flag ended = endedFlag(1L, 10L, null);
        Flag preserved = endedFlag(2L, 11L, null);
        givenCandidates(List.of(ended, preserved), Map.of(1L, List.of(21L), 2L, List.of(22L)));
        given(conclusionDecider.conclude(eq(preserved), any())).willReturn(FlagConclusionKind.PRESERVED);

        // when
        flagExpiryService.concludeEndedFlags();

        // then
        assertThat(publishedConclusions())
                .extracting(FlagConcludedEvent::flagId, FlagConcludedEvent::kind)
                .containsExactly(
                        tuple(1L, FlagConclusionKind.ENDED),
                        tuple(2L, FlagConclusionKind.PRESERVED));
    }

    @Test
    @DisplayName("참여자를 한 번에 조회해 플래그마다 종료 사실을 발행한다")
    void concludeEndedFlags_PublishesConclusionPerFlag() {
        // given
        givenCandidates(List.of(endedFlag(1L, 10L, null), endedFlag(2L, 11L, 99L)),
                Map.of(1L, List.of(21L, 22L), 2L, List.of(23L)));

        // when
        flagExpiryService.concludeEndedFlags();

        // then
        verify(flagRepository).findAllParticipantIdsByFlagIds(List.of(1L, 2L));

        List<FlagConcludedEvent> published = publishedConclusions();
        assertThat(published).hasSize(2);
        assertThat(published.get(0).flagId()).isEqualTo(1L);
        assertThat(published.get(0).hostId()).isEqualTo(10L);
        assertThat(published.get(0).participantIds()).containsExactly(21L, 22L);
        assertThat(published.get(0).isEncore()).isFalse();
        assertThat(published.get(1).participantIds()).containsExactly(23L);
        assertThat(published.get(1).isEncore()).isTrue();
        assertThat(published).allSatisfy(event ->
                assertThat(event.kind()).isEqualTo(FlagConclusionKind.ENDED));
    }

    @Test
    @DisplayName("참여자가 없는 플래그는 결말만 정하고 발행하지 않는다")
    void concludeEndedFlags_NoParticipants_PublishesNothing() {
        // given
        Flag flag = endedFlag(1L, 10L, null);
        givenCandidates(List.of(flag), Map.of());

        // when
        flagExpiryService.concludeEndedFlags();

        // then
        verify(flagRepository).save(flag);
        verify(eventPublisher, never()).publishEvent(any(FlagConcludedEvent.class));
    }

    @Test
    @DisplayName("만료 대상이 없으면 아무것도 발행하지 않는다")
    void concludeEndedFlags_NoTargets_PublishesNothing() {
        // given
        given(flagRepository.findConclusionCandidates(any())).willReturn(List.of());

        // when
        flagExpiryService.concludeEndedFlags();

        // then
        verify(eventPublisher, never()).publishEvent(any(FlagConcludedEvent.class));
    }

    @Test
    @DisplayName("한 회차가 가져가는 만료 대상 수에 상한이 없다")
    void concludeEndedFlags_HasNoBatchLimit() {
        // given
        List<Flag> manyTargets = IntStream.rangeClosed(1, 500)
                .mapToObj(i -> endedFlag((long) i, 10L, null))
                .toList();
        Map<Long, List<Long>> participants = manyTargets.stream()
                .collect(Collectors.toMap(Flag::getId, target -> List.of(21L)));
        givenCandidates(manyTargets, participants);

        // when
        flagExpiryService.concludeEndedFlags();

        // then
        assertThat(publishedConclusions()).hasSize(manyTargets.size());
        verify(conclusionDecider, times(manyTargets.size())).conclude(any(), any());
    }
}
