package com.example.DunbarHorizon.trace.application;

import com.example.DunbarHorizon.trace.application.port.in.TraceCommandUseCase;
import com.example.DunbarHorizon.trace.domain.model.Trace;
import com.example.DunbarHorizon.trace.domain.repository.TraceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;

import org.junit.jupiter.api.BeforeEach;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TraceServiceRetryTest.Config.class)
class TraceServiceRetryTest {

    @Configuration
    @EnableRetry
    static class Config {
        @Bean
        TraceRepository traceRepository() {
            return mock(TraceRepository.class);
        }

        @Bean
        TraceService traceService(TraceRepository traceRepository) {
            return new TraceService(traceRepository);
        }
    }

    @Autowired
    private TraceCommandUseCase traceService;

    @Autowired
    private TraceRepository traceRepository;

    private final Long user1 = 1L;
    private final Long user2 = 2L;

    @BeforeEach
    void resetMocks() {
        reset(traceRepository);
    }

    @Test
    @DisplayName("DataIntegrityViolationException 발생 시 최대 3회까지 재시도한다")
    void recordTrace_RetriesOnDataIntegrityViolation() {
        // given: 매 호출마다 INSERT 충돌 발생
        given(traceRepository.findByUserAIdAndUserBId(any(), any())).willReturn(Optional.empty());
        given(traceRepository.save(any())).willThrow(DataIntegrityViolationException.class);

        // when & then: 3회 재시도 후 예외 전파
        assertThatThrownBy(() -> traceService.recordTrace(user1, user2))
                .isInstanceOf(DataIntegrityViolationException.class);

        verify(traceRepository, times(3)).save(any(Trace.class));
    }

    @Test
    @DisplayName("ObjectOptimisticLockingFailureException 발생 시 최대 3회까지 재시도한다")
    void recordTrace_RetriesOnOptimisticLockingFailure() {
        // given: 매 호출마다 UPDATE 충돌 발생
        given(traceRepository.findByUserAIdAndUserBId(any(), any())).willReturn(Optional.empty());
        given(traceRepository.save(any())).willThrow(ObjectOptimisticLockingFailureException.class);

        // when & then: 3회 재시도 후 예외 전파
        assertThatThrownBy(() -> traceService.recordTrace(user1, user2))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);

        verify(traceRepository, times(3)).save(any(Trace.class));
    }

    @Test
    @DisplayName("첫 번째 시도에서 충돌이 발생해도 두 번째 시도에서 성공하면 정상 완료된다")
    void recordTrace_SucceedsOnSecondAttempt() {
        // given: 첫 번째 save는 충돌, 두 번째는 성공
        Trace existingTrace = new Trace(user1, user2);
        given(traceRepository.findByUserAIdAndUserBId(any(), any()))
                .willReturn(Optional.empty())
                .willReturn(Optional.of(existingTrace));
        given(traceRepository.save(any()))
                .willThrow(DataIntegrityViolationException.class)
                .willReturn(existingTrace);

        // when & then: 재시도 후 정상 완료
        traceService.recordTrace(user1, user2);

        verify(traceRepository, times(2)).save(any(Trace.class));
    }
}
