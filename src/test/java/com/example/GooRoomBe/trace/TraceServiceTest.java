package com.example.GooRoomBe.trace;

import com.example.GooRoomBe.global.event.interaction.UserInteractionEvent;
import com.example.GooRoomBe.trace.adapter.in.web.dto.TraceRecordResponseDto;
import com.example.GooRoomBe.trace.application.TraceService;
import com.example.GooRoomBe.trace.domain.event.TraceRevealedEvent;
import com.example.GooRoomBe.trace.domain.model.Trace;
import com.example.GooRoomBe.trace.domain.repository.TraceRepository;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TraceServiceTest {

    @InjectMocks
    private TraceService traceService;
    @Mock
    private TraceRepository traceRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private final Long visitorId = 1L;
    private final Long targetId = 2L;

    @Test
    @DisplayName("첫 방문 시 새로운 Trace를 생성하고 상호작용 이벤트를 발행한다")
    void recordTrace_FirstTime_Success() {
        // given
        given(traceRepository.findByVisitorAndTarget(visitorId, targetId)).willReturn(Optional.empty());
        given(traceRepository.findByVisitorAndTarget(targetId, visitorId)).willReturn(Optional.empty());

        // when
        TraceRecordResponseDto response = traceService.recordTrace(visitorId, targetId);

        // then
        assertThat(response.isMatched()).isEqualTo(false);
        verify(traceRepository).save(any(Trace.class));
        verify(eventPublisher).publishEvent(any(UserInteractionEvent.class));
        verify(eventPublisher, never()).publishEvent(any(TraceRevealedEvent.class));
    }

    @Test
    @DisplayName("서로 3회 이상 방문했을 경우 정체 공개 이벤트를 발행한다")
    void recordTrace_Reveal_Success() {
        // given
        // 내 방문 기록 (이미 2회였고 이번에 3회째가 된다고 가정)
        Trace myTrace = spy(new Trace(visitorId, targetId));
        ReflectionTestUtils.setField(myTrace, "id", 100L);
        ReflectionTestUtils.setField(myTrace, "count", 2);
        // 마지막 방문일은 어제로 설정하여 오늘 방문 시 카운트가 오르도록 유도
        ReflectionTestUtils.setField(myTrace, "lastVisitedAt", LocalDateTime.now().minusDays(1));

        // 상대방의 나에 대한 방문 기록 (이미 3회)
        Trace partnerTrace = new Trace(targetId, visitorId);
        ReflectionTestUtils.setField(partnerTrace, "count", 3);

        given(traceRepository.findByVisitorAndTarget(visitorId, targetId)).willReturn(Optional.of(myTrace));
        given(traceRepository.findByVisitorAndTarget(targetId, visitorId)).willReturn(Optional.of(partnerTrace));

        // when
        TraceRecordResponseDto response = traceService.recordTrace(visitorId, targetId);

        // then
        assertThat(response.isMatched()).isEqualTo(true);
        assertThat(myTrace.getCount()).isEqualTo(3);
        verify(eventPublisher).publishEvent(any(TraceRevealedEvent.class));
        verify(eventPublisher).publishEvent(any(UserInteractionEvent.class));
    }
}
