package com.example.GooRoomBe.trace.application;

import com.example.GooRoomBe.global.event.UserInteractionEvent;
import com.example.GooRoomBe.global.userReference.SocialUser;
import com.example.GooRoomBe.social.common.SocialUserPort;
import com.example.GooRoomBe.trace.api.dto.TraceRecordResponseDto;
import com.example.GooRoomBe.trace.application.TraceService;
import com.example.GooRoomBe.trace.domain.Trace;
import com.example.GooRoomBe.trace.domain.TracePort;
import com.example.GooRoomBe.trace.domain.event.TraceRevealedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TraceServiceTest {

    @Mock private TracePort tracePort;
    @Mock private SocialUserPort socialUserPort;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private TraceService traceService;

    private final String VISITOR_ID = "visitor";
    private final String TARGET_ID = "target";

    @Test
    @DisplayName("자가 방문 시 로직을 수행하지 않고 Hidden 반환")
    void visit_Self_ShouldReturnHidden() {
        TraceRecordResponseDto result = traceService.visit(VISITOR_ID, VISITOR_ID);

        assertThat(result.isMatched()).isFalse();
        verifyNoInteractions(tracePort);
    }

    @Test
    @DisplayName("신규 방문: 생성 및 저장은 하되, 공개 체크는 건너뛴다")
    void visit_New_ShouldCreateAndSkipCheck() {
        // Given
        given(tracePort.findTrace(VISITOR_ID, TARGET_ID)).willReturn(Optional.empty());

        // Mock Users
        SocialUser mockVisitor = mock(SocialUser.class);
        SocialUser mockTarget = mock(SocialUser.class);
        given(mockVisitor.getId()).willReturn(VISITOR_ID);
        given(mockTarget.getId()).willReturn(TARGET_ID);

        given(socialUserPort.getUser(VISITOR_ID)).willReturn(mockVisitor);
        given(socialUserPort.getUser(TARGET_ID)).willReturn(mockTarget);

        // When
        TraceRecordResponseDto result = traceService.visit(VISITOR_ID, TARGET_ID);

        // Then
        verify(tracePort).save(any(Trace.class), eq(VISITOR_ID));
        verify(eventPublisher).publishEvent(any(UserInteractionEvent.class));

        // 중요: 신규 방문은 절대 공개될 수 없으므로 partnerCount 조회를 안 함
        verify(tracePort, never()).getVisitCount(any(), any());
        assertThat(result.isMatched()).isFalse();
    }

    @Test
    @DisplayName("재방문 (공개 조건 미달): 카운트 업데이트 후 DB 조회 없이 종료")
    void visit_Revisit_NotReady_ShouldSkipDB() {
        // Given
        Trace mockTrace = mock(Trace.class);
        given(tracePort.findTrace(VISITOR_ID, TARGET_ID)).willReturn(Optional.of(mockTrace));

        // Mock 동작: 아직 공개 준비 안 됨
        given(mockTrace.isRevealReady()).willReturn(false);

        // When
        TraceRecordResponseDto result = traceService.visit(VISITOR_ID, TARGET_ID);

        // Then
        verify(mockTrace).updateVisitCount();
        verify(tracePort).save(mockTrace, VISITOR_ID);

        // isRevealReady가 false면 DB 조회를 안 함
        verify(tracePort, never()).getVisitCount(any(), any());
        assertThat(result.isMatched()).isFalse();
    }

    @Test
    @DisplayName("재방문 (공개 조건 달성): 상대방 카운트 조회 후 공개")
    void visit_Revisit_ReadyAndMatched_ShouldReveal() {
        // Given
        Trace mockTrace = mock(Trace.class);
        given(tracePort.findTrace(VISITOR_ID, TARGET_ID)).willReturn(Optional.of(mockTrace));

        // Mock 동작: 공개 준비 완료 & 이벤트 발생
        given(mockTrace.isRevealReady()).willReturn(true);
        given(tracePort.getVisitCount(TARGET_ID, VISITOR_ID)).willReturn(3); // 상대도 3회

        TraceRevealedEvent event = new TraceRevealedEvent(VISITOR_ID, "Nick", TARGET_ID);
        given(mockTrace.checkRevealEvent(3)).willReturn(Optional.of(event));

        // 응답 생성용 Target
        SocialUser mockTarget = mock(SocialUser.class);
        given(mockTrace.getTarget()).willReturn(mockTarget);

        // When
        TraceRecordResponseDto result = traceService.visit(VISITOR_ID, TARGET_ID);

        // Then
        verify(eventPublisher).publishEvent(event);
        assertThat(result.isMatched()).isTrue();
    }
}