package com.example.GooRoomBe.trace;

import com.example.GooRoomBe.trace.domain.event.TraceRevealedEvent;
import com.example.GooRoomBe.trace.domain.model.Trace;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

class TraceTest {

    private final Long visitorId = 1L;
    private final Long targetId = 2L;

    @Test
    @DisplayName("자기 자신을 방문하려고 하면 예외가 발생한다")
    void constructor_Fail_SelfVisit() {
        assertThatThrownBy(() -> new Trace(visitorId, visitorId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("자기 자신은 방문할 수 없습니다.");
    }

    @Test
    @DisplayName("첫 방문 시 카운트는 1이어야 한다")
    void constructor_Success() {
        Trace trace = new Trace(visitorId, targetId);

        assertThat(trace.getCount()).isEqualTo(1);
        assertThat(trace.getVisitorId()).isEqualTo(visitorId);
        assertThat(trace.getTargetId()).isEqualTo(targetId);
    }

    @Test
    @DisplayName("같은 날 다시 방문하면 카운트가 증가하지 않는다")
    void updateVisitCount_SameDay_NoIncrement() {
        // given
        Trace trace = new Trace(visitorId, targetId);
        int initialCount = trace.getCount();

        // when
        trace.updateVisitCount();

        // then
        assertThat(trace.getCount()).isEqualTo(initialCount);
    }

    @Test
    @DisplayName("7일이 지나서 방문하면 카운트가 1로 초기화된다")
    void updateVisitCount_Expired_ResetToOn() {
        // given: Reflection 등을 통해 과거 날짜 주입
        Trace trace = createOldTrace(visitorId, targetId, LocalDateTime.now().minusDays(8), 5);

        // when
        trace.updateVisitCount();

        // then
        assertThat(trace.getCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("서로 방문 횟수가 임계치(3회)를 넘으면 공개 이벤트가 생성된다")
    void checkRevealEvent_Success() {
        // given
        Trace trace = createOldTrace(visitorId, targetId, LocalDateTime.now().minusDays(1), 3);
        int partnerCount = 3;

        // when
        Optional<TraceRevealedEvent> event = trace.checkRevealEvent(partnerCount);

        // then
        assertThat(event).isPresent();
        assertThat(event.get().visitorId()).isEqualTo(visitorId);
        assertThat(event.get().targetId()).isEqualTo(targetId);
    }

    private Trace createOldTrace(Long vId, Long tId, LocalDateTime lastVisitedAt, int count) {
        Trace trace = new Trace(vId, tId);
        try {
            java.lang.reflect.Field countField = Trace.class.getDeclaredField("count");
            java.lang.reflect.Field dateField = Trace.class.getDeclaredField("lastVisitedAt");
            countField.setAccessible(true);
            dateField.setAccessible(true);
            countField.set(trace, count);
            dateField.set(trace, lastVisitedAt);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return trace;
    }
}