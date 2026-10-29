package com.example.DunbarHorizon.social.application.service;

import com.example.DunbarHorizon.global.event.interaction.InteractionType;
import com.example.DunbarHorizon.global.event.interaction.ScoreDirection;
import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.social.domain.friend.InteractionScorePolicy;
import com.example.DunbarHorizon.social.domain.friend.MutualInterestScoreUpdate;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendshipRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.TransientDataAccessResourceException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InteractionScoreServiceTest {

    private static final Long FLAG_ID = 100L;
    private static final Long HOST_ID = 10L;
    private static final List<Long> PARTICIPANT_IDS = List.of(21L, 22L);
    private static final InteractionType FLAG_TYPE = InteractionType.FLAG_ENDED;
    private static final double FLAG_DELTA = InteractionScorePolicy.scoreOf(FLAG_TYPE);

    @InjectMocks private InteractionScoreService interactionScoreService;
    @Mock private FriendshipRepository friendshipRepository;

    @Test
    @DisplayName("Flag 결론은 host-참가자와 참가자 쌍 전체를 하나의 batch로 갱신한다")
    void applyFlagConclusion_updatesAllPairsInOneBatch() {
        // when
        interactionScoreService.applyFlagConclusion(FLAG_ID, HOST_ID, PARTICIPANT_IDS, FLAG_TYPE, ScoreDirection.GRANT);

        // then
        ArgumentCaptor<List<MutualInterestScoreUpdate>> captor = ArgumentCaptor.forClass(List.class);
        verify(friendshipRepository).incrementMutualInterestScoresBatch(captor.capture());
        assertThat(captor.getValue()).containsExactlyInAnyOrder(
                new MutualInterestScoreUpdate(Friendship.generateCompositeId(HOST_ID, 21L), HOST_ID, 21L, FLAG_DELTA),
                new MutualInterestScoreUpdate(Friendship.generateCompositeId(HOST_ID, 22L), HOST_ID, 22L, FLAG_DELTA),
                new MutualInterestScoreUpdate(Friendship.generateCompositeId(21L, 22L), 21L, 22L, FLAG_DELTA)
        );
    }

    @Test
    @DisplayName("REVOKE는 같은 쌍에 같은 크기의 음수 델타를 넣는다")
    void applyFlagConclusion_revokeMirrorsGrantWithNegativeDelta() {
        // given
        InteractionType type = InteractionType.FLAG_PRESERVED;

        // when
        interactionScoreService.applyFlagConclusion(FLAG_ID, HOST_ID, PARTICIPANT_IDS, type, ScoreDirection.REVOKE);

        // then
        ArgumentCaptor<List<MutualInterestScoreUpdate>> captor = ArgumentCaptor.forClass(List.class);
        verify(friendshipRepository).incrementMutualInterestScoresBatch(captor.capture());
        double revoked = -InteractionScorePolicy.scoreOf(type);
        assertThat(captor.getValue()).containsExactlyInAnyOrder(
                new MutualInterestScoreUpdate(Friendship.generateCompositeId(HOST_ID, 21L), HOST_ID, 21L, revoked),
                new MutualInterestScoreUpdate(Friendship.generateCompositeId(HOST_ID, 22L), HOST_ID, 22L, revoked),
                new MutualInterestScoreUpdate(Friendship.generateCompositeId(21L, 22L), 21L, 22L, revoked)
        );
        assertThat(captor.getValue()).allSatisfy(update -> assertThat(update.delta()).isNegative());
    }

    @Test
    @DisplayName("rollback이 확인된 transient Flag batch 실패만 제한적으로 재시도한다")
    void applyFlagConclusion_retriesTransientFailure() {
        // given
        doThrow(new TransientDataAccessResourceException("deadlock"))
                .doNothing()
                .when(friendshipRepository).incrementMutualInterestScoresBatch(any());

        // when
        interactionScoreService.applyFlagConclusion(FLAG_ID, HOST_ID, PARTICIPANT_IDS, FLAG_TYPE, ScoreDirection.GRANT);

        // then
        verify(friendshipRepository, times(2)).incrementMutualInterestScoresBatch(any());
    }

    @Test
    @DisplayName("재시도를 소진한 Flag batch 실패는 삼키지 않고 호출자에게 올린다")
    void applyFlagConclusion_rethrowsAfterExhaustingRetries() {
        // given
        doThrow(new TransientDataAccessResourceException("deadlock"))
                .when(friendshipRepository).incrementMutualInterestScoresBatch(any());

        // when & then
        assertThatThrownBy(() -> interactionScoreService.applyFlagConclusion(FLAG_ID, HOST_ID, PARTICIPANT_IDS, FLAG_TYPE, ScoreDirection.GRANT))
                .isInstanceOf(TransientDataAccessResourceException.class);
        verify(friendshipRepository, times(3)).incrementMutualInterestScoresBatch(any());
    }

    @Test
    @DisplayName("비일시적 Flag batch 실패는 재시도하지 않고 바로 올린다")
    void applyFlagConclusion_rethrowsNonTransientFailureImmediately() {
        // given
        doThrow(new IllegalStateException("not transient"))
                .when(friendshipRepository).incrementMutualInterestScoresBatch(any());

        // when & then
        assertThatThrownBy(() -> interactionScoreService.applyFlagConclusion(FLAG_ID, HOST_ID, PARTICIPANT_IDS, FLAG_TYPE, ScoreDirection.GRANT))
                .isInstanceOf(IllegalStateException.class);
        verify(friendshipRepository, times(1)).incrementMutualInterestScoresBatch(any());
    }

    @Test
    @DisplayName("mutual 타입은 양방향 커맨드를 타입에 맞는 델타로 부른다")
    void applyInteraction_mutualTypeCallsBidirectionalCommand() {
        // given
        InteractionType type = InteractionType.FLAG_ENDED_ENCORE;

        // when
        interactionScoreService.applyInteraction(1L, 2L, type);

        // then
        verify(friendshipRepository).incrementMutualInterestScore(
                Friendship.generateCompositeId(1L, 2L), 1L, 2L, InteractionScorePolicy.scoreOf(type));
    }

    @Test
    @DisplayName("단방향 타입은 한쪽 커맨드를 타입에 맞는 델타로 부른다")
    void applyInteraction_unilateralTypeCallsOneSidedCommand() {
        // given
        InteractionType type = InteractionType.VISIT;

        // when
        interactionScoreService.applyInteraction(1L, 2L, type);

        // then
        verify(friendshipRepository).incrementInterestScore(
                Friendship.generateCompositeId(1L, 2L), 1L, 2L, InteractionScorePolicy.scoreOf(type));
    }

    @Test
    @DisplayName("단건 증분도 rollback이 확인된 transient 실패를 재시도한다")
    void applyInteraction_retriesTransientFailure() {
        // given
        doThrow(new TransientDataAccessResourceException("deadlock"))
                .doNothing()
                .when(friendshipRepository).incrementInterestScore(any(), any(), any(), anyDouble());

        // when
        interactionScoreService.applyInteraction(1L, 2L, InteractionType.VISIT);

        // then
        verify(friendshipRepository, times(2)).incrementInterestScore(any(), any(), any(), anyDouble());
    }

    @Test
    @DisplayName("단건 증분이 재시도를 소진하면 삼키지 않고 호출자에게 올린다")
    void applyInteraction_rethrowsAfterExhaustingRetries() {
        // given
        doThrow(new TransientDataAccessResourceException("deadlock"))
                .when(friendshipRepository).incrementInterestScore(any(), any(), any(), anyDouble());

        // when & then
        assertThatThrownBy(() -> interactionScoreService.applyInteraction(1L, 2L, InteractionType.VISIT))
                .isInstanceOf(TransientDataAccessResourceException.class);
        verify(friendshipRepository, times(3)).incrementInterestScore(any(), any(), any(), anyDouble());
    }

    @Test
    @DisplayName("단건 증분의 비일시적 실패는 재시도하지 않고 바로 올린다")
    void applyInteraction_rethrowsNonTransientFailureImmediately() {
        // given
        doThrow(new IllegalStateException("not transient"))
                .when(friendshipRepository).incrementInterestScore(any(), any(), any(), anyDouble());

        // when & then
        assertThatThrownBy(() -> interactionScoreService.applyInteraction(1L, 2L, InteractionType.VISIT))
                .isInstanceOf(IllegalStateException.class);
        verify(friendshipRepository, times(1)).incrementInterestScore(any(), any(), any(), anyDouble());
    }
}
