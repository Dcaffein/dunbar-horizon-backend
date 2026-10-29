package com.example.DunbarHorizon.flag.application.eventListener;

import com.example.DunbarHorizon.flag.domain.flag.event.FlagConcludedEvent;
import com.example.DunbarHorizon.flag.domain.flag.event.FlagConclusionKind;
import com.example.DunbarHorizon.global.event.interaction.BatchMutualInteractionEvent;
import com.example.DunbarHorizon.global.event.interaction.InteractionType;
import com.example.DunbarHorizon.global.event.interaction.ScoreDirection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FlagConclusionEventListenerTest {

    @InjectMocks
    private FlagConclusionEventListener listener;

    @Mock private ApplicationEventPublisher eventPublisher;

    private static final Long FLAG_ID = 1L;
    private static final Long HOST_ID = 10L;
    private static final Long PARENT_ID = 99L;
    private static final List<Long> PARTICIPANT_IDS = List.of(21L, 22L, 23L);

    private BatchMutualInteractionEvent capturedInteraction() {
        ArgumentCaptor<BatchMutualInteractionEvent> captor =
                ArgumentCaptor.forClass(BatchMutualInteractionEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("이벤트에 실려온 참여자와 호스트를 그대로 상호작용 이벤트로 넘긴다")
    void handleFlagConclusion_forwardsParticipants() {
        // when
        listener.handleFlagConclusion(new FlagConcludedEvent(
                FLAG_ID, HOST_ID, null, PARTICIPANT_IDS, FlagConclusionKind.ENDED));

        // then
        BatchMutualInteractionEvent event = capturedInteraction();
        assertThat(event.flagId()).isEqualTo(FLAG_ID);
        assertThat(event.participantIds()).containsExactlyElementsOf(PARTICIPANT_IDS);
        assertThat(event.hostId()).isEqualTo(HOST_ID);
    }

    @ParameterizedTest
    @CsvSource({
            "ENDED,                 false, FLAG_ENDED,            GRANT",
            "ENDED,                 true,  FLAG_ENDED_ENCORE,     GRANT",
            "PRESERVED,             false, FLAG_PRESERVED,        GRANT",
            "PRESERVED,             true,  FLAG_PRESERVED_ENCORE, GRANT",
            "PRESERVATION_RELEASED, false, FLAG_PRESERVED,        REVOKE",
            "PRESERVATION_RELEASED, true,  FLAG_PRESERVED_ENCORE, REVOKE"
    })
    @DisplayName("종료의 종류와 앵코르 여부로 점수 타입과 방향을 고른다")
    void handleFlagConclusion_translatesKindAndEncore(FlagConclusionKind kind,
                                                      boolean encore,
                                                      InteractionType expectedType,
                                                      ScoreDirection expectedDirection) {
        // given
        Long parentId = encore ? PARENT_ID : null;

        // when
        listener.handleFlagConclusion(
                new FlagConcludedEvent(FLAG_ID, HOST_ID, parentId, PARTICIPANT_IDS, kind));

        // then
        BatchMutualInteractionEvent event = capturedInteraction();
        assertThat(event.type()).isEqualTo(expectedType);
        assertThat(event.direction()).isEqualTo(expectedDirection);
    }

    @Test
    @DisplayName("보존 부여와 보존 해제는 같은 타입을 반대 방향으로 쓴다")
    void handleFlagConclusion_grantAndRevokeShareTheSameType() {
        // when
        listener.handleFlagConclusion(new FlagConcludedEvent(
                FLAG_ID, HOST_ID, null, PARTICIPANT_IDS, FlagConclusionKind.PRESERVED));
        listener.handleFlagConclusion(new FlagConcludedEvent(
                FLAG_ID, HOST_ID, null, PARTICIPANT_IDS, FlagConclusionKind.PRESERVATION_RELEASED));

        // then
        ArgumentCaptor<BatchMutualInteractionEvent> captor =
                ArgumentCaptor.forClass(BatchMutualInteractionEvent.class);
        verify(eventPublisher, times(2)).publishEvent(captor.capture());
        BatchMutualInteractionEvent granted = captor.getAllValues().get(0);
        BatchMutualInteractionEvent revoked = captor.getAllValues().get(1);
        assertThat(revoked.type()).isEqualTo(granted.type());
        assertThat(granted.direction()).isEqualTo(ScoreDirection.GRANT);
        assertThat(revoked.direction()).isEqualTo(ScoreDirection.REVOKE);
    }

    @Test
    @DisplayName("참여자 없는 종료 사실은 만들 수 없다")
    void flagConcludedEvent_rejectsEmptyParticipants() {
        assertThatThrownBy(() -> new FlagConcludedEvent(
                FLAG_ID, HOST_ID, null, List.of(), FlagConclusionKind.ENDED))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new FlagConcludedEvent(
                FLAG_ID, HOST_ID, null, null, FlagConclusionKind.ENDED))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
