package com.example.DunbarHorizon.social.application.eventListener;

import com.example.DunbarHorizon.global.annotation.Neo4jTransactional;
import com.example.DunbarHorizon.social.domain.friend.event.FriendShipDeletedEvent;
import com.example.DunbarHorizon.social.domain.label.repository.LabelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class LabelMemberShipEventListener {

    private final LabelRepository labelRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Neo4jTransactional(propagation = Propagation.REQUIRES_NEW)
    public void handleFriendShipDeleted(FriendShipDeletedEvent event) {
        labelRepository.removeMemberFromOwnedLabels(event.userAId(), event.userBId());
        labelRepository.removeMemberFromOwnedLabels(event.userBId(), event.userAId());
    }
}
