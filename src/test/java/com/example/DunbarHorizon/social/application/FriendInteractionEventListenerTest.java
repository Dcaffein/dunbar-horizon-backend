package com.example.DunbarHorizon.social.application;

import com.example.DunbarHorizon.global.event.interaction.BatchMutualInteractionEvent;
import com.example.DunbarHorizon.global.event.interaction.InteractionType;
import com.example.DunbarHorizon.global.event.interaction.UserInteractionEvent;
import com.example.DunbarHorizon.social.application.eventListener.FriendInteractionEventListener;
import com.example.DunbarHorizon.social.application.port.out.InteractionScoreDeltaPort;
import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.social.domain.friend.InteractionScorePolicy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class FriendInteractionEventListenerTest {

    @InjectMocks
    private FriendInteractionEventListener listener;

    @Mock
    private InteractionScoreDeltaPort deltaPort;

    private static final Long USER_A = 1L;
    private static final Long USER_B = 2L;
    private static final Long HOST = 10L;

    @Test
    @DisplayName("mutual=false 타입 수신 시 userA 방향으로 accumulate를 호출한다")
    void handleUserInteraction_unilateral_callsAccumulate() {
        UserInteractionEvent event = new UserInteractionEvent(USER_A, USER_B, InteractionType.VISIT);
        String friendshipId = Friendship.generateCompositeId(USER_A, USER_B);
        double delta = InteractionScorePolicy.scoreOf(InteractionType.VISIT);

        listener.handleUserInteraction(event);

        verify(deltaPort).accumulate(friendshipId, USER_A, delta);
    }

    @Test
    @DisplayName("mutual=true 타입 수신 시 accumulateMutual을 호출한다")
    void handleUserInteraction_mutual_callsAccumulateMutual() {
        UserInteractionEvent event = new UserInteractionEvent(USER_A, USER_B, InteractionType.FLAG_ENDED);
        String friendshipId = Friendship.generateCompositeId(USER_A, USER_B);
        double delta = InteractionScorePolicy.scoreOf(InteractionType.FLAG_ENDED);

        listener.handleUserInteraction(event);

        verify(deltaPort).accumulateMutual(friendshipId, delta);
    }

    @Test
    @DisplayName("배치 이벤트 수신 시 호스트↔참여자, 참여자 간 모든 쌍에 accumulateMutual을 호출한다")
    void handleBatchMutualInteraction_callsAccumulateMutualForAllPairs() {
        List<Long> participants = List.of(USER_A, USER_B);
        BatchMutualInteractionEvent event = new BatchMutualInteractionEvent(participants, HOST, InteractionType.FLAG_ENDED);
        double delta = InteractionScorePolicy.scoreOf(InteractionType.FLAG_ENDED);

        listener.handleBatchMutualInteraction(event);

        verify(deltaPort).accumulateMutual(Friendship.generateCompositeId(HOST, USER_A), delta);
        verify(deltaPort).accumulateMutual(Friendship.generateCompositeId(HOST, USER_B), delta);
        verify(deltaPort).accumulateMutual(Friendship.generateCompositeId(USER_A, USER_B), delta);
        verify(deltaPort, times(3)).accumulateMutual(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyDouble());
    }
}
