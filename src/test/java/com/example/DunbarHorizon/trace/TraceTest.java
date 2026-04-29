package com.example.DunbarHorizon.trace;

import com.example.DunbarHorizon.global.event.interaction.InteractionType;
import com.example.DunbarHorizon.global.event.interaction.UserInteractionEvent;
import com.example.DunbarHorizon.trace.domain.event.TraceRevealedEvent;
import com.example.DunbarHorizon.trace.domain.model.Trace;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TraceTest {

    private final Long user1 = 1L;
    private final Long user2 = 2L;

    @Test
    @DisplayName("자기 자신을 방문하려고 하면 예외가 발생한다")
    void constructor_Fail_SelfVisit() {
        assertThatThrownBy(() -> new Trace(user1, user1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot trace self.");
    }

    @Test
    @DisplayName("생성 시 ID가 작은 값이 userA, 큰 값이 userB로 정렬되어 단일 로우를 보장한다")
    void constructor_SortsIds() {
        // given: 큰 ID(2L)가 방문자, 작은 ID(1L)가 타겟으로 생성
        Trace trace = new Trace(user2, user1);

        // then: 무조건 작은 값이 userA로 고정됨
        assertThat(trace.getUserAId()).isEqualTo(user1);
        assertThat(trace.getUserBId()).isEqualTo(user2);

        // 2번 유저(B)가 방문했으므로 B의 카운트가 1, A는 0
        assertThat(trace.getUserACount()).isEqualTo(0);
        assertThat(trace.getUserBCount()).isEqualTo(1);
        assertThat(trace.getUserBLastVisitedAt()).isNotNull();
        assertThat(trace.getUserALastVisitedAt()).isNull();
    }

    @Test
    @DisplayName("같은 날 동일한 유저가 다시 방문하면 카운트가 증가하지 않고 이벤트도 발생하지 않는다")
    void recordVisit_SameDay_NoIncrement() {
        // given
        Trace trace = new Trace(user1, user2); // A(1L)의 첫 방문 (카운트 1)
        clearDomainEvents(trace); // 생성 시 발생한 이벤트 지우기

        // when: 같은 날 다시 방문
        trace.recordVisit(user1);

        // then
        assertThat(trace.getUserACount()).isEqualTo(1); // 카운트 증가 안 함
        assertThat(getDomainEvents(trace)).isEmpty(); // 상호작용 이벤트 무시됨
    }

    @Test
    @DisplayName("상대방이 방문하면 독립적으로 카운트가 증가하고 상호작용 이벤트가 정상 적재된다")
    void recordVisit_PartnerVisit_Success() {
        // given
        Trace trace = new Trace(user1, user2); // A가 방문
        clearDomainEvents(trace);

        // when: B가 방문
        trace.recordVisit(user2);

        // then: B의 카운트만 증가
        assertThat(trace.getUserACount()).isEqualTo(1);
        assertThat(trace.getUserBCount()).isEqualTo(1);

        // 친밀도를 높이는 UserInteractionEvent 정상 적재 확인
        Collection<Object> events = getDomainEvents(trace);
        assertThat(events).hasSize(1);

        Object event = events.iterator().next();
        assertThat(event).isInstanceOf(UserInteractionEvent.class);
        UserInteractionEvent interactionEvent = (UserInteractionEvent) event;
        assertThat(interactionEvent.type()).isEqualTo(InteractionType.VISIT);
    }

    @Test
    @DisplayName("서로 모르는 상태에서 마지막 방문 후 3일이 지나면 만료 처리되어 방문 기록이 거부된다")
    void recordVisit_Expired_TrackB_ThrowsException() {
        // given
        Trace trace = new Trace(user1, user2);

        // 마지막 방문일을 4일 전으로 조작 (TRACING_EXPIRATION_DAYS = 3)
        ReflectionTestUtils.setField(trace, "lastTracedAt", LocalDateTime.now().minusDays(4));

        // when & then
        assertThatThrownBy(() -> trace.recordVisit(user2))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("This trace has already expired.");
    }

    @Test
    @DisplayName("서로 공개된 상태에서 7일이 지나면 만료 상태가 된다")
    void isExpired_TrackA_ReturnsTrue() {
        // given
        Trace trace = new Trace(user1, user2);

        // 공개 상태 조작
        ReflectionTestUtils.setField(trace, "isRevealed", true);

        // 공개일을 8일 전으로 조작 (REVEALED_EXPIRATION_DAYS = 7)
        ReflectionTestUtils.setField(trace, "revealedAt", LocalDateTime.now().minusDays(8));

        // when & then
        assertThat(trace.isExpired()).isTrue();
    }

    @Test
    @DisplayName("양측 방문 횟수가 모두 3회에 도달하면 상태가 변경되고 TraceRevealedEvent가 적재된다")
    void recordVisit_RevealConditionMet_RegistersEvent() {
        // given
        Trace trace = new Trace(user1, user2);

        // A는 3회, B는 2회 방문한 상태로 조작
        ReflectionTestUtils.setField(trace, "userACount", 3);
        ReflectionTestUtils.setField(trace, "userBCount", 2);

        // 오늘 방문 제한을 피하기 위해 B의 마지막 방문일을 어제로 조작
        ReflectionTestUtils.setField(trace, "userBLastVisitedAt", LocalDateTime.now().minusDays(1));
        clearDomainEvents(trace);

        // when: B가 3번째 방문 달성
        trace.recordVisit(user2);

        // then: 도메인 상태 변경 확인
        assertThat(trace.isRevealed()).isTrue();
        assertThat(trace.getRevealedAt()).isNotNull();

        // 도메인 이벤트 검증 (방문 이벤트 1개 + 호감 공개 이벤트 1개)
        Collection<Object> events = getDomainEvents(trace);
        assertThat(events).hasSize(2);

        long revealedEventCount = events.stream()
                .filter(event -> event instanceof TraceRevealedEvent)
                .count();
        assertThat(revealedEventCount).isEqualTo(1);
    }

    @Test
    @DisplayName("이미 공개된 발자국은 3회 이상의 방문이 계속되어도 공개 이벤트를 중복 발행하지 않는다")
    void recordVisit_AlreadyRevealed_NoDuplicateRevealEvent() {
        // given
        Trace trace = new Trace(user1, user2);
        ReflectionTestUtils.setField(trace, "userACount", 4);
        ReflectionTestUtils.setField(trace, "userBCount", 3);
        ReflectionTestUtils.setField(trace, "isRevealed", true);
        ReflectionTestUtils.setField(trace, "userBLastVisitedAt", LocalDateTime.now().minusDays(1));
        clearDomainEvents(trace);

        // when: B가 4번째 방문
        trace.recordVisit(user2);

        // then: 방문 이벤트만 발행되고 TraceRevealedEvent는 발행되지 않음
        Collection<Object> events = getDomainEvents(trace);
        assertThat(events).hasSize(1);
        assertThat(events.iterator().next()).isInstanceOf(UserInteractionEvent.class);
    }

    @SuppressWarnings("unchecked")
    private Collection<Object> getDomainEvents(Trace trace) {
        return ReflectionTestUtils.invokeMethod(trace, "domainEvents");
    }

    private void clearDomainEvents(Trace trace) {
        ReflectionTestUtils.invokeMethod(trace, "clearDomainEvents");
    }
}