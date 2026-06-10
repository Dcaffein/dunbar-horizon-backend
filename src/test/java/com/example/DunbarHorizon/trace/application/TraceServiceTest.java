package com.example.DunbarHorizon.trace.application;

import com.example.DunbarHorizon.trace.application.dto.TraceResult;
import com.example.DunbarHorizon.trace.domain.model.Trace;
import com.example.DunbarHorizon.trace.domain.repository.TraceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TraceServiceTest {

    @InjectMocks
    private TraceService traceService;

    @Mock
    private TraceRepository traceRepository;

    private final Long user1 = 1L;
    private final Long user2 = 2L;

    @Test
    @DisplayName("기존 흔적이 없으면 새로운 Trace를 생성하고 방문을 기록한 뒤 저장한다")
    void recordTrace_NewTrace_Success() {
        // given
        given(traceRepository.findByUserAIdAndUserBId(user1, user2)).willReturn(Optional.empty());

        // when
        TraceResult result = traceService.recordTrace(user1, user2);

        // then
        verify(traceRepository).save(any(Trace.class));
        assertThat(result.revealed()).isFalse();
    }

    @Test
    @DisplayName("기존 흔적이 존재하면 해당 객체에 방문 기록을 위임하고 변경 사항을 저장한다")
    void recordTrace_ExistingTrace_DelegatesToDomain() {
        // given
        Trace mockTrace = spy(new Trace(user1, user2));
        given(traceRepository.findByUserAIdAndUserBId(user1, user2)).willReturn(Optional.of(mockTrace));

        // when
        TraceResult result = traceService.recordTrace(user1, user2);

        // then
        verify(mockTrace).recordVisit(user1);
        verify(traceRepository).save(mockTrace);
        assertThat(result.revealed()).isFalse();
    }

    @Test
    @DisplayName("방문으로 reveal 조건이 충족되면 justRevealed: true와 상대 유저 ID를 반환한다")
    void recordTrace_WhenRevealed_ReturnsJustRevealed() {
        // given
        Trace trace = spy(new Trace(user1, user2));
        ReflectionTestUtils.setField(trace, "userACount", 2);
        ReflectionTestUtils.setField(trace, "userBCount", 3);
        ReflectionTestUtils.setField(trace, "userALastVisitedAt", java.time.LocalDateTime.now().minusDays(1));
        given(traceRepository.findByUserAIdAndUserBId(user1, user2)).willReturn(Optional.of(trace));

        // when
        TraceResult result = traceService.recordTrace(user1, user2);

        // then
        assertThat(result.revealed()).isTrue();
    }

    @Test
    @DisplayName("이미 reveal된 상태에서 재방문하면 revealed: false를 반환한다")
    void recordTrace_AlreadyRevealed_ReturnsFalse() {
        // given
        Trace trace = spy(new Trace(user1, user2));
        ReflectionTestUtils.setField(trace, "userACount", 3);
        ReflectionTestUtils.setField(trace, "userBCount", 3);
        ReflectionTestUtils.setField(trace, "isRevealed", true);
        given(traceRepository.findByUserAIdAndUserBId(user1, user2)).willReturn(Optional.of(trace));

        // when
        TraceResult result = traceService.recordTrace(user1, user2);

        // then
        assertThat(result.revealed()).isFalse();
    }
}
