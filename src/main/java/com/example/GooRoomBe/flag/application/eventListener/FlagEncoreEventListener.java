package com.example.GooRoomBe.flag.application.eventListener;

import com.example.GooRoomBe.flag.domain.flag.event.FlagEncoreEvent;
import com.example.GooRoomBe.flag.domain.flag.repository.FlagParticipantRepository;
import com.example.GooRoomBe.global.event.notification.NotificationEvent;
import com.example.GooRoomBe.global.event.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class FlagEncoreEventListener {

    private final FlagParticipantRepository participantRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(FlagEncoreEvent event) {
        List<Long> oldParticipantIds = participantRepository.findAllParticipantIdsByFlagId(event.parentFlagId());

        if (oldParticipantIds.isEmpty()) return;

        NotificationEvent notificationEvent = NotificationEvent.builder()
                .receiverIds(oldParticipantIds)
                .title("앵콜 플래그 생성!")
                .content(String.format("[%s] 플래그가 다시 열렸습니다. 지금 확인해보세요!", event.title()))
                .type(NotificationType.FLAG_ENCORE)
                .metadata(Map.of(
                        "parentFlagId", event.parentFlagId()
                ))
                .build();

        eventPublisher.publishEvent(notificationEvent);
    }
}