package com.example.DunbarHorizon.social.application.eventListener;

import com.example.DunbarHorizon.social.domain.friend.event.FriendShipDeletedEvent;
import com.example.DunbarHorizon.social.domain.label.Label;
import com.example.DunbarHorizon.social.domain.label.repository.LabelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class LabelMemberShipEventListener {

    private final LabelRepository labelRepository;

    @EventListener
    @Transactional
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
