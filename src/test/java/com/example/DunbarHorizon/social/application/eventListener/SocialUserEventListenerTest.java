package com.example.DunbarHorizon.social.application.eventListener;

import com.example.DunbarHorizon.account.domain.outbox.UserOutboxEventType;
import com.example.DunbarHorizon.global.event.user.UserSyncCompletedEvent;
import com.example.DunbarHorizon.global.event.user.UserSyncIntegrationEvent;
import com.example.DunbarHorizon.social.application.service.SocialUserSyncCommandService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.TransactionSystemException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class SocialUserEventListenerTest {

    @InjectMocks private SocialUserEventListener listener;
    @Mock private SocialUserSyncCommandService syncCommandService;
    @Mock private ApplicationEventPublisher eventPublisher;

    @Test
    @DisplayName("트랜잭션 서비스가 성공 반환한 뒤에만 완료 이벤트를 발행한다")
    void onUserSync_ServiceReturns_PublishesCompletedAfterSync() {
        // given
        UserSyncIntegrationEvent event = activationEvent();

        // when
        listener.onUserSync(event);

        // then
        var order = inOrder(syncCommandService, eventPublisher);
        order.verify(syncCommandService).sync(event);
        order.verify(eventPublisher).publishEvent(new UserSyncCompletedEvent(event.outboxId()));
    }

    @Test
    @DisplayName("트랜잭션 프록시의 커밋 실패가 전파되면 완료 이벤트를 발행하지 않는다")
    void onUserSync_CommitFails_DoesNotPublishCompleted() {
        // given
        UserSyncIntegrationEvent event = activationEvent();
        willThrow(new TransactionSystemException("Neo4j commit failed"))
                .given(syncCommandService).sync(event);

        // when
        listener.onUserSync(event);

        // then
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("그래프 처리 실패 시 완료 이벤트를 발행하지 않는다")
    void onUserSync_ProcessingFails_DoesNotPublishCompleted() {
        // given
        UserSyncIntegrationEvent event = activationEvent();
        willThrow(new IllegalStateException("Neo4j write failed"))
                .given(syncCommandService).sync(event);

        // when
        listener.onUserSync(event);

        // then
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("완료 처리 실패도 수신부에서 처리하여 재시도에 맡긴다")
    void onUserSync_CompletionFails_DoesNotPropagate() {
        // given
        UserSyncIntegrationEvent event = activationEvent();
        willThrow(new TransactionSystemException("MySQL completion failed"))
                .given(eventPublisher).publishEvent(new UserSyncCompletedEvent(event.outboxId()));

        // when & then
        assertThatCode(() -> listener.onUserSync(event)).doesNotThrowAnyException();
    }

    private UserSyncIntegrationEvent activationEvent() {
        return new UserSyncIntegrationEvent("outbox-1", 1L,
                UserOutboxEventType.ACTIVATE, "nick", null, null);
    }
}
