package com.example.DunbarHorizon.flag.application.eventListener;

import com.example.DunbarHorizon.flag.domain.flag.Flag;
import com.example.DunbarHorizon.flag.domain.flag.FlagPreservationCriteria;
import com.example.DunbarHorizon.flag.domain.flag.FlagStatus;
import com.example.DunbarHorizon.flag.domain.flag.repository.FlagParticipantRepository;
import com.example.DunbarHorizon.flag.domain.flag.event.FlagDeletedEvent;
import com.example.DunbarHorizon.flag.domain.flag.repository.FlagRepository;
import com.example.DunbarHorizon.flag.domain.memorial.repository.FlagMemorialRepository;
import com.example.DunbarHorizon.global.event.interaction.InteractionType;
import com.example.DunbarHorizon.global.event.interaction.MutualInteractionEvent;
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
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class FlagDeletionEventListener {

    private final FlagRepository flagRepository;
    private final FlagParticipantRepository participantRepository;
    private final FlagMemorialRepository memorialRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleFlagDeletion(FlagDeletedEvent event) {
        Flag deletedFlag = flagRepository.findById(event.flagId())
                .orElseThrow(() -> new IllegalStateException("삭제된 플래그를 찾을 수 없습니다."));

        Optional<Flag> encoreResult = flagRepository.findByParentId(deletedFlag.getId());
        encoreResult.ifPresent(Flag::severParentLink);

        processParticipantCleanup(event, deletedFlag.getHostId());

        if (deletedFlag.getParentId() != null) {
            flagRepository.findById(deletedFlag.getParentId()).ifPresent(grandParent -> {
                boolean hasMemorial = memorialRepository.existsByFlagId(deletedFlag.getId());
                boolean hasEncore = encoreResult.isPresent();
                grandParent.updatePreservation(new FlagPreservationCriteria(hasMemorial,hasEncore));
            });
        }
    }

    private void processParticipantCleanup(FlagDeletedEvent event, Long hostId) {
        List<Long> participantIds = participantRepository.findAllParticipantIdsByFlagId(event.flagId());

        if (!participantIds.isEmpty() && event.statusAtDeletion() != FlagStatus.RECRUITING) {
            publishNotification(participantIds, event.flagTitle());
        }

        if (!participantIds.isEmpty() && isMeetingHeld(event.statusAtDeletion())) {
            publishInteractionEvents(participantIds, hostId, event.parentId() != null);
        }

        participantRepository.deleteAllByFlagId(event.flagId());
    }

    private boolean isMeetingHeld(FlagStatus status) {
        return status == FlagStatus.ENDED || status == FlagStatus.IN_ACTIVITY;
    }

    private void publishInteractionEvents(List<Long> participantIds, Long hostId, boolean isEncore) {
        InteractionType type = isEncore ? InteractionType.FLAG_ENDED_ENCORE : InteractionType.FLAG_ENDED;

        participantIds.forEach(participantId ->
                eventPublisher.publishEvent(new MutualInteractionEvent(hostId, participantId, type))
        );

        for (int i = 0; i < participantIds.size(); i++) {
            for (int j = i + 1; j < participantIds.size(); j++) {
                eventPublisher.publishEvent(new MutualInteractionEvent(
                        participantIds.get(i), participantIds.get(j), type
                ));
            }
        }
    }

    private void publishNotification(List<Long> receiverIds, String title) {
        NotificationEvent notificationEvent = NotificationEvent.builder()
                .receiverIds(receiverIds)
                .title("모임 취소 안내")
                .content(String.format("[%s] 모임이 호스트 사정으로 취소되었습니다.", title))
                .type(NotificationType.FLAG_CANCELED)
                .occurredAt(LocalDateTime.now())
                .build();

        eventPublisher.publishEvent(notificationEvent);
    }
}