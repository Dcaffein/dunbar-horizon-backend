package com.example.DunbarHorizon.flag.application.eventListener;

import com.example.DunbarHorizon.flag.domain.flag.event.FlagMeetingChangedEvent;
import com.example.DunbarHorizon.flag.domain.flag.repository.FlagParticipantRepository;
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

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class FlagMeetingChangedEventListener {

    private final FlagParticipantRepository participantRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(FlagMeetingChangedEvent event) {
        List<Long> participantIds = participantRepository.findAllParticipantIdsByFlagId(event.flagId());

        if (participantIds.isEmpty()) return;

        NotificationEvent notificationEvent = NotificationEvent.builder()
                .receiverIds(participantIds)
                .title("모임 시간 변경 안내")
                .content(String.format("[%s] 모임 시간이 변경되었습니다. 새로운 일정을 확인해주세요!", event.flagTitle()))
                .type(NotificationType.FLAG_SCHEDULE_CHANGED)
                .occurredAt(LocalDateTime.now())
                .build();

        eventPublisher.publishEvent(notificationEvent);
    }
}
