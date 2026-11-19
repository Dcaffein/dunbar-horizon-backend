package com.example.DunbarHorizon.social.application.friendship;

import com.example.DunbarHorizon.global.event.interaction.InteractionType;
import com.example.DunbarHorizon.global.event.interaction.ScoreDirection;
import com.example.DunbarHorizon.social.domain.friendship.InterestScoreDecayParameters;
import com.example.DunbarHorizon.social.application.friendship.port.FriendshipScoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 재시도와 트랜잭션은 프록시가 거는 것이라 Spring 컨텍스트에서 본다.
 * 시도마다 트랜잭션이 새로 열리는지(재시도 프록시가 트랜잭션 프록시 바깥인지)가 핵심이다.
 */
@SpringJUnitConfig(FriendshipScoreServiceRetryTest.Config.class)
class FriendshipScoreServiceRetryTest {

    private static final InteractionType FLAG_TYPE = InteractionType.FLAG_ENDED;

    @Configuration
    @EnableRetry
    @EnableTransactionManagement
    @Import(FriendshipScoreService.class)
    static class Config {
        @Bean
        PlatformTransactionManager transactionManager() {
            return mock(PlatformTransactionManager.class);
        }
    }

    @Autowired private FriendshipScoreService friendshipScoreService;
    @Autowired private PlatformTransactionManager transactionManager;
    @MockitoBean private FriendshipScoreRepository friendshipScoreRepository;
    @MockitoBean private InterestScoreDecayParameters decayParameters;

    @BeforeEach
    void setUp() {
        reset(transactionManager);
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
    }

    @Test
    @DisplayName("일시적 실패는 시도마다 새 트랜잭션을 열어 재시도한다")
    void applyFlagConclusion_RetriesInNewTransaction_TransientFailure() {
        // given
        when(friendshipScoreRepository.findAllAmongForUpdate(anyCollection()))
                .thenThrow(new TransientDataAccessResourceException("deadlock"))
                .thenReturn(List.of());

        // when
        friendshipScoreService.applyFlagConclusion(10L, List.of(21L, 22L), FLAG_TYPE, ScoreDirection.GRANT);

        // then
        verify(friendshipScoreRepository, times(2)).findAllAmongForUpdate(anyCollection());
        verify(transactionManager, times(2)).getTransaction(any());
        verify(transactionManager).rollback(any());
        verify(transactionManager).commit(any());
    }

    @Test
    @DisplayName("커밋 시점에 DB가 던진 일시적 실패도 재시도한다")
    void applyInteraction_Retries_TransientFailureOnCommit() {
        // given
        when(friendshipScoreRepository.findBetweenForUpdate(anyLong(), anyLong())).thenReturn(Optional.empty());
        doThrow(new TransientDataAccessResourceException("deadlock"))
                .doNothing()
                .when(transactionManager).commit(any());

        // when
        friendshipScoreService.applyInteraction(1L, 2L, InteractionType.VISIT);

        // then
        verify(transactionManager, times(2)).getTransaction(any());
        verify(transactionManager, times(2)).commit(any());
    }

    @Test
    @DisplayName("재시도를 소진한 실패는 삼키지 않고 호출자에게 올린다")
    void applyInteraction_Rethrows_RetriesExhausted() {
        // given
        when(friendshipScoreRepository.findBetweenForUpdate(anyLong(), anyLong()))
                .thenThrow(new TransientDataAccessResourceException("deadlock"));

        // when & then
        assertThatThrownBy(() -> friendshipScoreService.applyInteraction(1L, 2L, InteractionType.VISIT))
                .isInstanceOf(TransientDataAccessResourceException.class);
        verify(friendshipScoreRepository, times(3)).findBetweenForUpdate(anyLong(), anyLong());
        verify(transactionManager, times(3)).rollback(any());
    }

    @Test
    @DisplayName("비일시적 실패는 재시도하지 않고 바로 올린다")
    void applyInteraction_RethrowsImmediately_NonTransientFailure() {
        // given
        when(friendshipScoreRepository.findBetweenForUpdate(anyLong(), anyLong()))
                .thenThrow(new IllegalStateException("not transient"));

        // when & then
        assertThatThrownBy(() -> friendshipScoreService.applyInteraction(1L, 2L, InteractionType.VISIT))
                .isInstanceOf(IllegalStateException.class);
        verify(friendshipScoreRepository, times(1)).findBetweenForUpdate(anyLong(), anyLong());
        verify(transactionManager, never()).commit(any());
    }
}
