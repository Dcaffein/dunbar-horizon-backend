package com.example.GooRoomBe.social.trace.domain;

import com.example.GooRoomBe.social.socialUser.SocialUser;
import com.example.GooRoomBe.social.trace.domain.event.TraceRevealedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TraceTest {

    private SocialUser visitor;
    private SocialUser target;

    @BeforeEach
    void setUp() {
        visitor = mock(SocialUser.class);
        target = mock(SocialUser.class);

        when(visitor.getId()).thenReturn("visitor-id");
        when(target.getId()).thenReturn("target-id");
        when(visitor.getNickname()).thenReturn("visitor-nick");
    }

    @Test
    @DisplayName("생성자: 자가 방문(Visitor == Target) 시 예외가 발생한다")
    void constructor_SelfVisit_ShouldThrowException() {
        // Given
        when(visitor.getId()).thenReturn("same-id");
        when(target.getId()).thenReturn("same-id");

        // When & Then
        assertThatThrownBy(() -> new Trace(visitor, target))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("같은 날 방문하면 카운트는 증가하지 않고 시간만 갱신된다")
    void updateVisitCount_SameDay_ShouldNotIncreaseCount() {
        // Given
        // 현재 시간이 아니라 '고정된 시간(낮 12시)'을 기준점으로 잡음
        LocalDateTime fixedNow = LocalDateTime.of(2025, 1, 1, 12, 0, 0);
        LocalDateTime pastTime = fixedNow.minusHours(1); // 같은 날 오전 11시

        // Trace 생성 (마지막 방문: 오전 11시)
        Trace trace = new Trace("id", visitor, target, 1, pastTime);

        // When
        // 고정된 시간(낮 12시)을 주입해서 업데이트
        trace.updateVisitCount(fixedNow);

        // Then
        assertThat(trace.getCount()).isEqualTo(1); // 카운트 유지 (1)
        assertThat(trace.getLastVisitedAt()).isEqualTo(fixedNow); // 시간 갱신 확인
    }

    @Test
    @DisplayName("다른 날(다음 날) 방문하면 카운트가 증가한다")
    void updateVisitCount_NextDay_ShouldIncreaseCount() {
        // Given
        LocalDateTime fixedNow = LocalDateTime.of(2025, 1, 2, 12, 0, 0);
        LocalDateTime yesterday = fixedNow.minusDays(1); // 1월 1일

        Trace trace = new Trace("id", visitor, target, 1, yesterday);

        // When
        trace.updateVisitCount(fixedNow);

        // Then
        assertThat(trace.getCount()).isEqualTo(2); // 1 -> 2
    }

    @Test
    @DisplayName("7일이 지나면 만료되어 카운트가 1로 초기화된다")
    void updateVisitCount_Expired_ShouldResetCount() {
        // Given: 8일 전 방문, 카운트 5
        LocalDateTime longTimeAgo = LocalDateTime.now().minusDays(8);
        Trace trace = new Trace("id", visitor, target, 5, longTimeAgo);

        // When
        trace.updateVisitCount();

        // Then
        assertThat(trace.getCount()).isEqualTo(1); // 초기화
    }

    @Test
    @DisplayName("공개 조건: 내 카운트가 3이고 상대 카운트도 3 이상이면 이벤트가 발생한다")
    void checkRevealEvent_ShouldReturnEvent_OnExactMoment() {
        // Given: 내 카운트 3으로 설정
        Trace trace = new Trace("id", visitor, target, 3, LocalDateTime.now());

        // When: 상대 카운트 3 전달
        Optional<TraceRevealedEvent> event = trace.checkRevealEvent(3);

        // Then
        assertThat(event).isPresent();
        assertThat(event.get().visitorId()).isEqualTo("visitor-id");
    }

    @Test
    @DisplayName("공개 조건 미달: 내 카운트가 부족하거나 넘쳤을 때, 혹은 상대가 부족할 때")
    void checkRevealEvent_ShouldReturnEmpty() {
        // Case 1: 내 카운트 부족 (2)
        Trace trace1 = new Trace("id", visitor, target, 2, LocalDateTime.now());
        assertThat(trace1.checkRevealEvent(3)).isEmpty();

        // Case 2: 내 카운트 초과 (4) - 이미 공개되었으므로 이벤트 발생 안 함
        Trace trace2 = new Trace("id", visitor, target, 4, LocalDateTime.now());
        assertThat(trace2.checkRevealEvent(3)).isEmpty();

        // Case 3: 내 카운트 충족(3) but 상대 부족(2)
        Trace trace3 = new Trace("id", visitor, target, 3, LocalDateTime.now());
        assertThat(trace3.checkRevealEvent(2)).isEmpty();
    }
}