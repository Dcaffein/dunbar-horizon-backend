package com.example.DunbarHorizon.flag.application.eventListener;

import com.example.DunbarHorizon.flag.domain.invitation.event.FlagInvitationSentEvent;
import com.example.DunbarHorizon.global.event.notification.NotificationEvent;
import com.example.DunbarHorizon.global.event.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class FlagInvitationEventListener {

    private final ApplicationEventPublisher eventPublisher;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(FlagInvitationSentEvent event) {
        String title = event.isEncore() ? "앵콜 초대" : "플래그 초대";
        String content = event.isEncore()
                ? String.format("[%s] 앵콜 모임에 초대받았습니다. 함께하실래요!", event.flagTitle())
                : String.format("[%s] 플래그에 초대받았습니다. 수락 여부를 선택해주세요!", event.flagTitle());

        NotificationEvent notificationEvent = NotificationEvent.builder()
                .receiverIds(List.of(event.inviteeId()))
                .title(title)
                .content(content)
                .type(NotificationType.FLAG_INVITATION)
                .metadata(Map.of(
                        "flagId", event.flagId(),
                        "invitationId", event.invitationId()
                ))
                .build();

        eventPublisher.publishEvent(notificationEvent);
    }
}
