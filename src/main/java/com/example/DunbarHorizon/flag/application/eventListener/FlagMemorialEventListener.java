package com.example.DunbarHorizon.flag.application.eventListener;

import com.example.DunbarHorizon.flag.domain.flag.FlagPreservationPolicy;
import com.example.DunbarHorizon.flag.domain.memorial.event.MemorialCreatedEvent;
import com.example.DunbarHorizon.flag.domain.memorial.event.MemorialDeletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class FlagMemorialEventListener {

    private final FlagPreservationPolicy flagPreservationPolicy;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleMemorialCreated(MemorialCreatedEvent event) {
        flagPreservationPolicy.refresh(event.flagId());
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleMemorialDeleted(MemorialDeletedEvent event) {
        flagPreservationPolicy.refresh(event.flagId());
    }
}
