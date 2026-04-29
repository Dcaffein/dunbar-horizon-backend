package com.example.DunbarHorizon.trace.application;

import com.example.DunbarHorizon.trace.adapter.in.web.dto.TraceRecordResponseDto;
import com.example.DunbarHorizon.trace.domain.model.Trace;
import com.example.DunbarHorizon.trace.domain.repository.TraceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
        // given: 리포지토리에 데이터가 없는 상황 (1L, 2L 순서로 조회됨)
        given(traceRepository.findByUserAIdAndUserBId(user1, user2))
                .willReturn(Optional.empty());

        // when
        TraceRecordResponseDto response = traceService.recordTrace(user1, user2);

        // then
        verify(traceRepository).save(any(Trace.class)); // 새로운 객체가 저장되었는지 확인
        assertThat(response.isRevealed()).isFalse();   // 초기 상태이므로 false 반환 확인
    }

    @Test
    @DisplayName("기존 흔적이 존재하면 해당 객체에 방문 기록을 위임하고 변경 사항을 저장한다")
    void recordTrace_ExistingTrace_DelegatesToDomain() {
        // given: 이미 존재하는 Trace 모킹
        Trace mockTrace = spy(new Trace(user1, user2));
        given(traceRepository.findByUserAIdAndUserBId(user1, user2))
                .willReturn(Optional.of(mockTrace));

        // when
        traceService.recordTrace(user1, user2);

        // then
        verify(mockTrace).recordVisit(user1); // 도메인 로직(recordVisit)이 호출되었는지 확인
        verify(traceRepository).save(mockTrace); // 변경된 객체가 저장되었는지 확인
    }

    @Test
    @DisplayName("도메인 객체가 공개(Revealed) 상태라면 응답 DTO에 반영되어야 한다")
    void recordTrace_WhenRevealed_ReturnsRevealedDto() {
        // given: 공개 조건이 이미 충족된 상태의 Trace 가정
        Trace revealedTrace = new Trace(user1, user2);
        // 리플렉션 등을 사용하지 않고도 테스트 가능하도록 도메인 상태를 조작하거나
        // 실제 recordVisit을 여러번 호출한 객체를 준비할 수 있습니다.
        for(int i=0; i<3; i++) {
            // 내부 로직상 날짜 제한이 있다면 spy나 별도 조작이 필요할 수 있음
        }

        // 여기서는 단순하게 공개 상태로 가정된 mock을 사용
        Trace mockTrace = mock(Trace.class);
        given(mockTrace.isRevealed()).willReturn(true);
        given(traceRepository.findByUserAIdAndUserBId(anyLong(), anyLong()))
                .willReturn(Optional.of(mockTrace));

        // when
        TraceRecordResponseDto response = traceService.recordTrace(user1, user2);

        // then
        assertThat(response.isRevealed()).isTrue();
    }
}