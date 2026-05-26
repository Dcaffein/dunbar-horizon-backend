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
    @DisplayName("mutual=false 타입 수신 시 userA 방향으로만 delta를 누적한다")
    void handleUserInteraction_unilateral_accumulatesOnlyUserA() {
        UserInteractionEvent event = new UserInteractionEvent(USER_A, USER_B, InteractionType.VISIT);
        String friendshipId = Friendship.generateCompositeId(USER_A, USER_B);
        double delta = InteractionScorePolicy.scoreOf(InteractionType.VISIT);

        listener.handleUserInteraction(event);

        verify(deltaPort).accumulate(friendshipId, USER_A, delta);
        verify(deltaPort, times(1)).accumulate(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyDouble());
    }

    @Test
    @DisplayName("mutual=true 타입 수신 시 userA·userB 양쪽 모두 delta를 누적한다")
    void handleUserInteraction_mutual_accumulatesBothSides() {
        UserInteractionEvent event = new UserInteractionEvent(USER_A, USER_B, InteractionType.FLAG_ENDED);
        String friendshipId = Friendship.generateCompositeId(USER_A, USER_B);
        double delta = InteractionScorePolicy.scoreOf(InteractionType.FLAG_ENDED);

        listener.handleUserInteraction(event);

        verify(deltaPort).accumulate(friendshipId, USER_A, delta);
        verify(deltaPort).accumulate(friendshipId, USER_B, delta);
    }

    @Test
    @DisplayName("배치 이벤트 수신 시 호스트↔참여자, 참여자 간 모든 쌍에 delta를 누적한다")
    void handleBatchMutualInteraction_accumulatesAllPairs() {
        List<Long> participants = List.of(USER_A, USER_B);
        BatchMutualInteractionEvent event = new BatchMutualInteractionEvent(participants, HOST, InteractionType.FLAG_ENDED);
        double delta = InteractionScorePolicy.scoreOf(InteractionType.FLAG_ENDED);

        listener.handleBatchMutualInteraction(event);

        // HOST <-> USER_A
        String hostA = Friendship.generateCompositeId(HOST, USER_A);
        verify(deltaPort).accumulate(hostA, HOST, delta);
        verify(deltaPort).accumulate(hostA, USER_A, delta);

        // HOST <-> USER_B
        String hostB = Friendship.generateCompositeId(HOST, USER_B);
        verify(deltaPort).accumulate(hostB, HOST, delta);
        verify(deltaPort).accumulate(hostB, USER_B, delta);

        // USER_A <-> USER_B
        String ab = Friendship.generateCompositeId(USER_A, USER_B);
        verify(deltaPort).accumulate(ab, USER_A, delta);
        verify(deltaPort).accumulate(ab, USER_B, delta);

        // 총 6번 accumulate
        verify(deltaPort, times(6)).accumulate(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyDouble());
    }
}
