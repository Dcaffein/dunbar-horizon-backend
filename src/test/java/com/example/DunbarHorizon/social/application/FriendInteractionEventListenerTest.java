package com.example.DunbarHorizon.social.application;

import com.example.DunbarHorizon.global.event.interaction.BatchMutualInteractionEvent;
import com.example.DunbarHorizon.global.event.interaction.InteractionType;
import com.example.DunbarHorizon.global.event.interaction.UserInteractionEvent;
import com.example.DunbarHorizon.social.application.eventListener.FriendInteractionEventListener;
import com.example.DunbarHorizon.social.application.service.InteractionScoreService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FriendInteractionEventListenerTest {

    @InjectMocks
    private FriendInteractionEventListener listener;

    @Mock
    private InteractionScoreService interactionScoreService;

    private static final Long USER_A = 1L;
    private static final Long USER_B = 2L;
    private static final Long HOST = 10L;

    @Test
    @DisplayName("단건 상호작용 이벤트는 두 사용자와 타입을 그대로 서비스에 넘긴다")
    void handleUserInteraction_delegatesUsersAndTypeToService() {
        UserInteractionEvent event = new UserInteractionEvent(USER_A, USER_B, InteractionType.VISIT);

        listener.handleUserInteraction(event);

        verify(interactionScoreService).applyInteraction(USER_A, USER_B, InteractionType.VISIT);
    }

    @Test
    @DisplayName("배치 이벤트는 참가자 전체와 타입을 그대로 서비스에 넘긴다")
    void handleBatchMutualInteraction_delegatesAllParticipantsAndTypeToService() {
        List<Long> participants = List.of(USER_A, USER_B);
        Long flagId = 91L;
        BatchMutualInteractionEvent event =
                new BatchMutualInteractionEvent(flagId, participants, HOST, InteractionType.FLAG_ENDED);

        listener.handleBatchMutualInteraction(event);

        verify(interactionScoreService).applyFlagConclusion(flagId, HOST, participants, InteractionType.FLAG_ENDED);
    }
}
