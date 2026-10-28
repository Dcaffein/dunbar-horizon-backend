package com.example.DunbarHorizon.social.application.service;

import com.example.DunbarHorizon.social.domain.friend.Friendship;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class IntimacyScoreManagerTest {

    private static final Long FLAG_ID = 100L;
    private static final Long HOST_ID = 10L;
    private static final List<Long> PARTICIPANT_IDS = List.of(21L, 22L);

    @InjectMocks private IntimacyScoreManager intimacyScoreManager;
    @Mock private FriendshipRepository friendshipRepository;

    @Test
    @DisplayName("Flag 결론은 host-참가자와 참가자 쌍 전체를 하나의 batch로 갱신한다")
    void applyFlagConclusion_updatesAllPairsInOneBatch() {
        // when
        intimacyScoreManager.applyFlagConclusion(FLAG_ID, HOST_ID, PARTICIPANT_IDS, 10.0);

        // then
        ArgumentCaptor<List<MutualInterestScoreUpdate>> captor = ArgumentCaptor.forClass(List.class);
        verify(friendshipRepository).incrementMutualInterestScoresBatch(captor.capture());
        assertThat(captor.getValue()).containsExactlyInAnyOrder(
                new MutualInterestScoreUpdate(Friendship.generateCompositeId(HOST_ID, 21L), HOST_ID, 21L, 10.0),
                new MutualInterestScoreUpdate(Friendship.generateCompositeId(HOST_ID, 22L), HOST_ID, 22L, 10.0),
                new MutualInterestScoreUpdate(Friendship.generateCompositeId(21L, 22L), 21L, 22L, 10.0)
        );
    }

    @Test
    @DisplayName("rollback이 확인된 transient Flag batch 실패만 제한적으로 재시도한다")
    void applyFlagConclusion_retriesTransientFailure() {
        // given
        doThrow(new TransientDataAccessResourceException("deadlock"))
                .doNothing()
                .when(friendshipRepository).incrementMutualInterestScoresBatch(any());

        // when
        intimacyScoreManager.applyFlagConclusion(FLAG_ID, HOST_ID, PARTICIPANT_IDS, 10.0);

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
        assertThatThrownBy(() -> intimacyScoreManager.applyFlagConclusion(FLAG_ID, HOST_ID, PARTICIPANT_IDS, 10.0))
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
        assertThatThrownBy(() -> intimacyScoreManager.applyFlagConclusion(FLAG_ID, HOST_ID, PARTICIPANT_IDS, 10.0))
                .isInstanceOf(IllegalStateException.class);
        verify(friendshipRepository, times(1)).incrementMutualInterestScoresBatch(any());
    }
}
