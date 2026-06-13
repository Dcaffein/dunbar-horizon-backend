package com.example.DunbarHorizon.social.application.eventListener;

import com.example.DunbarHorizon.global.annotation.Neo4jTransactional;
import com.example.DunbarHorizon.social.domain.friend.event.FriendShipDeletedEvent;
import com.example.DunbarHorizon.social.domain.label.Label;
import com.example.DunbarHorizon.social.domain.label.repository.LabelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class LabelMemberShipEventListener {

    private final LabelRepository labelRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Neo4jTransactional
    public void handleFriendShipDeleted(FriendShipDeletedEvent event) {
        removeFriendFromLabels(event.userAId(), event.userBId());
        removeFriendFromLabels(event.userBId(), event.userAId());
    }

    private void removeFriendFromLabels(Long ownerId, Long memberIdToRemove) {
        List<Label> labels = labelRepository.findLabelsByOwnerAndMember(ownerId, memberIdToRemove);
        for (Label label : labels) {
            label.getMembers().stream()
                    .filter(m -> m.getId().equals(memberIdToRemove))
                    .findFirst()
                    .ifPresent(label::removeMember);
        }
        if (!labels.isEmpty()) {
            labelRepository.saveAll(labels);
        }
    }
}
