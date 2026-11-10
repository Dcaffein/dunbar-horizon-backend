package com.example.DunbarHorizon.flag.application.eventListener;

import com.example.DunbarHorizon.flag.domain.flag.event.FlagConcludedEvent;
import com.example.DunbarHorizon.global.event.interaction.BatchMutualInteractionEvent;
import com.example.DunbarHorizon.global.event.interaction.InteractionType;
import com.example.DunbarHorizon.global.event.interaction.ScoreDirection;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * flag의 종료 사실을 social의 상호작용으로 옮기는 유일한 지점이다.
 * 다른 곳에서 InteractionType을 고르기 시작하면 점수 기준이 흩어진다.
 */
@Component
@RequiredArgsConstructor
public class FlagConclusionEventListener {

    private final ApplicationEventPublisher eventPublisher;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleFlagConclusion(FlagConcludedEvent event) {
        InteractionType type = switch (event.kind()) {
            case ENDED -> event.isEncore()
                    ? InteractionType.FLAG_ENDED_ENCORE
                    : InteractionType.FLAG_ENDED;
            case PRESERVED -> event.isEncore()
                    ? InteractionType.FLAG_PRESERVED_ENCORE
                    : InteractionType.FLAG_PRESERVED;
        };

        eventPublisher.publishEvent(new BatchMutualInteractionEvent(
                event.flagId(), event.participantIds(), event.hostId(), type, ScoreDirection.GRANT));
    }
}
