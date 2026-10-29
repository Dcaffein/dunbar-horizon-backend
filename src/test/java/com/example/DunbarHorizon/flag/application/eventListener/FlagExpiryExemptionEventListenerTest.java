package com.example.DunbarHorizon.flag.application.eventListener;

import com.example.DunbarHorizon.flag.domain.flag.event.FlagConcludedEvent;
import com.example.DunbarHorizon.flag.domain.flag.event.FlagConclusionKind;
import com.example.DunbarHorizon.flag.domain.flag.event.FlagExpiryExemptionChangedEvent;
import com.example.DunbarHorizon.flag.domain.flag.repository.FlagRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FlagExpiryExemptionEventListenerTest {

    @InjectMocks
    private FlagExpiryExemptionEventListener listener;

    @Mock private FlagRepository flagRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    private static final Long FLAG_ID = 1L;
    private static final Long HOST_ID = 10L;
    private static final List<Long> PARTICIPANT_IDS = List.of(21L, 22L);

    private FlagConcludedEvent capturedConclusion() {
        ArgumentCaptor<FlagConcludedEvent> captor = ArgumentCaptor.forClass(FlagConcludedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("만료 면제가 붙으면 참여자를 조회해 보존된 종료 사실을 발행한다")
    void handleExpiryExemptionChanged_exempted_publishesPreserved() {
        // given
        given(flagRepository.findAllParticipantIds(FLAG_ID)).willReturn(PARTICIPANT_IDS);

        // when
        listener.handleExpiryExemptionChanged(
                new FlagExpiryExemptionChangedEvent(FLAG_ID, HOST_ID, 99L, true));

        // then
        FlagConcludedEvent concluded = capturedConclusion();
        assertThat(concluded.flagId()).isEqualTo(FLAG_ID);
        assertThat(concluded.hostId()).isEqualTo(HOST_ID);
        assertThat(concluded.participantIds()).containsExactlyElementsOf(PARTICIPANT_IDS);
        assertThat(concluded.isEncore()).isTrue();
        assertThat(concluded.kind()).isEqualTo(FlagConclusionKind.PRESERVED);
    }

    @Test
    @DisplayName("만료 면제가 떨어지면 보존 해제를 발행한다")
    void handleExpiryExemptionChanged_released_publishesRelease() {
        // given
        given(flagRepository.findAllParticipantIds(FLAG_ID)).willReturn(PARTICIPANT_IDS);

        // when
        listener.handleExpiryExemptionChanged(
                new FlagExpiryExemptionChangedEvent(FLAG_ID, HOST_ID, null, false));

        // then
        FlagConcludedEvent concluded = capturedConclusion();
        assertThat(concluded.participantIds()).containsExactlyElementsOf(PARTICIPANT_IDS);
        assertThat(concluded.isEncore()).isFalse();
        assertThat(concluded.kind()).isEqualTo(FlagConclusionKind.PRESERVATION_RELEASED);
    }

    @Test
    @DisplayName("참여자가 없으면 어느 방향으로도 발행하지 않는다")
    void handleExpiryExemptionChanged_noParticipants_publishesNothing() {
        // given
        given(flagRepository.findAllParticipantIds(FLAG_ID)).willReturn(List.of());

        // when
        listener.handleExpiryExemptionChanged(
                new FlagExpiryExemptionChangedEvent(FLAG_ID, HOST_ID, null, true));
        listener.handleExpiryExemptionChanged(
                new FlagExpiryExemptionChangedEvent(FLAG_ID, HOST_ID, null, false));

        // then
        verify(eventPublisher, never()).publishEvent(any(FlagConcludedEvent.class));
    }
}
