package com.example.DunbarHorizon.social.application;

import com.example.DunbarHorizon.global.event.interaction.BatchMutualInteractionEvent;
import com.example.DunbarHorizon.global.event.interaction.InteractionType;
import com.example.DunbarHorizon.global.event.interaction.UserInteractionEvent;
import com.example.DunbarHorizon.social.application.eventListener.FriendInteractionEventListener;
import com.example.DunbarHorizon.social.application.service.IntimacyScoreManager;
import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.social.domain.friend.InteractionScorePolicy;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendshipRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class FriendInteractionEventListenerTest {

    @InjectMocks
    private FriendInteractionEventListener listener;

    @Mock
    private FriendshipRepository friendshipRepository;

    @Mock
    private IntimacyScoreManager intimacyScoreManager;

    private static final Long USER_A = 1L;
    private static final Long USER_B = 2L;
    private static final Long HOST = 10L;

    @Test
    @DisplayName("mutual=false 타입 수신 시 userA 방향의 Cypher score 갱신을 호출한다")
    void handleUserInteraction_unilateral_updatesOneSideImmediately() {
        UserInteractionEvent event = new UserInteractionEvent(USER_A, USER_B, InteractionType.VISIT);
        String friendshipId = Friendship.generateCompositeId(USER_A, USER_B);
        double delta = InteractionScorePolicy.scoreOf(InteractionType.VISIT);

        listener.handleUserInteraction(event);

        verify(friendshipRepository).incrementInterestScore(friendshipId, USER_A, USER_B, delta);
    }

    @Test
    @DisplayName("mutual=true 타입 수신 시 양 방향 Cypher score 갱신을 호출한다")
    void handleUserInteraction_mutual_updatesBothSidesImmediately() {
        UserInteractionEvent event = new UserInteractionEvent(USER_A, USER_B, InteractionType.FLAG_ENDED);
        String friendshipId = Friendship.generateCompositeId(USER_A, USER_B);
        double delta = InteractionScorePolicy.scoreOf(InteractionType.FLAG_ENDED);

        listener.handleUserInteraction(event);

        verify(friendshipRepository).incrementMutualInterestScore(friendshipId, USER_A, USER_B, delta);
    }

    @Test
    @DisplayName("배치 이벤트 수신 시 Flag score manager에 전체 참가자를 전달한다")
    void handleBatchMutualInteraction_delegatesAllParticipantsToManager() {
        List<Long> participants = List.of(USER_A, USER_B);
        Long flagId = 91L;
        BatchMutualInteractionEvent event = new BatchMutualInteractionEvent(flagId, participants, HOST, InteractionType.FLAG_ENDED);
        double delta = InteractionScorePolicy.scoreOf(InteractionType.FLAG_ENDED);

        listener.handleBatchMutualInteraction(event);

        verify(intimacyScoreManager).enqueueFlagConclusion(flagId, HOST, participants, delta);
        verify(friendshipRepository, never()).incrementMutualInterestScore(
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyDouble());
    }
}
