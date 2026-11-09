package com.example.DunbarHorizon.social.application.service;

import com.example.DunbarHorizon.global.event.interaction.InteractionType;
import com.example.DunbarHorizon.global.event.interaction.ScoreDirection;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendshipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.exceptions.TransientException;
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
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
@SpringJUnitConfig(InteractionScoreServiceRetryTest.Config.class)
class InteractionScoreServiceRetryTest {

    private static final InteractionType FLAG_TYPE = InteractionType.FLAG_ENDED;

    @Configuration
    @EnableRetry
    @EnableTransactionManagement
    @Import(InteractionScoreService.class)
    static class Config {
        @Bean
        PlatformTransactionManager neo4jTransactionManager() {
            return mock(PlatformTransactionManager.class);
        }
    }

    @Autowired private InteractionScoreService interactionScoreService;
    @Autowired private PlatformTransactionManager neo4jTransactionManager;
    @MockitoBean private FriendshipRepository friendshipRepository;

    @BeforeEach
    void setUp() {
        reset(neo4jTransactionManager);
        when(neo4jTransactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
    }

    @Test
    @DisplayName("일시적 실패는 시도마다 새 트랜잭션을 열어 재시도한다")
    void applyFlagConclusion_RetriesInNewTransaction_TransientFailure() {
        // given
        when(friendshipRepository.findAllByIdsForUpdate(anyCollection()))
                .thenThrow(new TransientDataAccessResourceException("deadlock"))
                .thenReturn(List.of());

        // when
        interactionScoreService.applyFlagConclusion(1L, 10L, List.of(21L, 22L), FLAG_TYPE, ScoreDirection.GRANT);

        // then
        verify(friendshipRepository, times(2)).findAllByIdsForUpdate(anyCollection());
        verify(neo4jTransactionManager, times(2)).getTransaction(any());
        verify(neo4jTransactionManager).rollback(any());
        verify(neo4jTransactionManager).commit(any());
    }

    @Test
    @DisplayName("커밋 시점에 드라이버가 던진 일시적 실패도 재시도한다")
    void applyInteraction_Retries_TransientFailureOnCommit() {
        // given
        when(friendshipRepository.findAllByIdsForUpdate(anyCollection())).thenReturn(List.of());
        doThrow(new TransientException("Neo.TransientError.Transaction.DeadlockDetected", "deadlock"))
                .doNothing()
                .when(neo4jTransactionManager).commit(any());

        // when
        interactionScoreService.applyInteraction(1L, 2L, InteractionType.VISIT);

        // then
        verify(neo4jTransactionManager, times(2)).getTransaction(any());
        verify(neo4jTransactionManager, times(2)).commit(any());
    }

    @Test
    @DisplayName("재시도를 소진한 실패는 삼키지 않고 호출자에게 올린다")
    void applyInteraction_Rethrows_RetriesExhausted() {
        // given
        when(friendshipRepository.findAllByIdsForUpdate(anyCollection()))
                .thenThrow(new TransientDataAccessResourceException("deadlock"));

        // when & then
        assertThatThrownBy(() -> interactionScoreService.applyInteraction(1L, 2L, InteractionType.VISIT))
                .isInstanceOf(TransientDataAccessResourceException.class);
        verify(friendshipRepository, times(3)).findAllByIdsForUpdate(anyCollection());
        verify(neo4jTransactionManager, times(3)).rollback(any());
    }

    @Test
    @DisplayName("비일시적 실패는 재시도하지 않고 바로 올린다")
    void applyInteraction_RethrowsImmediately_NonTransientFailure() {
        // given
        when(friendshipRepository.findAllByIdsForUpdate(anyCollection()))
                .thenThrow(new IllegalStateException("not transient"));

        // when & then
        assertThatThrownBy(() -> interactionScoreService.applyInteraction(1L, 2L, InteractionType.VISIT))
                .isInstanceOf(IllegalStateException.class);
        verify(friendshipRepository, times(1)).findAllByIdsForUpdate(anyCollection());
        verify(neo4jTransactionManager, never()).commit(any());
    }
}
