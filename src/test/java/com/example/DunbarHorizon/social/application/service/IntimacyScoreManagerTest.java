package com.example.DunbarHorizon.social.application.service;

import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.social.domain.friend.FriendshipDecayPolicy;
import com.example.DunbarHorizon.social.domain.friend.MutualInterestScoreUpdate;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendshipRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.TransientDataAccessResourceException;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.doAnswer;
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
    @Mock private FriendshipDecayPolicy decayPolicy;

    @AfterEach
    void shutDownQueue() {
        intimacyScoreManager.shutdown();
    }

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
    @DisplayName("Flag batch가 끝난 뒤에만 decay가 같은 multi-Friendship 쓰기를 시작한다")
    void applyDecay_waitsForFlagBatchCompletion() throws Exception {
        // given
        CountDownLatch flagEntered = new CountDownLatch(1);
        CountDownLatch releaseFlag = new CountDownLatch(1);
        CountDownLatch decayEntered = new CountDownLatch(1);
        AtomicInteger activeWriters = new AtomicInteger();
        doAnswer(invocation -> {
            activeWriters.incrementAndGet();
            flagEntered.countDown();
            releaseFlag.await();
            activeWriters.decrementAndGet();
            return null;
        }).when(friendshipRepository).incrementMutualInterestScoresBatch(any());
        doAnswer(invocation -> {
            assertThat(activeWriters.get()).isZero();
            decayEntered.countDown();
            return null;
        }).when(friendshipRepository).applyDecay(anyDouble(), anyDouble(), any());

        intimacyScoreManager.enqueueFlagConclusion(FLAG_ID, HOST_ID, PARTICIPANT_IDS, 10.0);
        assertThat(flagEntered.await(1, TimeUnit.SECONDS)).isTrue();

        intimacyScoreManager.enqueueDecay();
        assertThat(decayEntered.await(100, TimeUnit.MILLISECONDS)).isFalse();
        releaseFlag.countDown();

        assertThat(decayEntered.await(1, TimeUnit.SECONDS)).isTrue();
    }
}
