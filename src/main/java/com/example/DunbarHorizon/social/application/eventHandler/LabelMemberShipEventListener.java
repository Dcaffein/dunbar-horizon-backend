package com.example.DunbarHorizon.social.application.eventHandler;

import com.example.DunbarHorizon.social.domain.friend.event.FriendShipDeletedEvent;
import com.example.DunbarHorizon.social.domain.label.Label;
import com.example.DunbarHorizon.social.domain.label.repository.LabelRepository;
import com.example.DunbarHorizon.social.domain.socialUser.repository.SocialUserRepository;
import com.example.DunbarHorizon.social.domain.socialUser.UserReference;
import com.example.DunbarHorizon.social.domain.socialUser.exception.UserReferenceNotFoundException;
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
    private final SocialUserRepository socialUserRepository;

    @EventListener
    @Transactional
    public void handleFriendShipDeleted(FriendShipDeletedEvent event) {
        removeFriendFromLabels(event.userAId(), event.userBId());
        removeFriendFromLabels(event.userBId(), event.userAId());
    }

    private void removeFriendFromLabels(Long ownerId, Long memberIdToRemove) {
        List<Label> labels = labelRepository.findAllByOwner_Id(ownerId);

        UserReference memberToRemove = socialUserRepository.findById(memberIdToRemove)
                .orElseThrow(() -> new UserReferenceNotFoundException(memberIdToRemove));

        for (Label label : labels) {
            log.debug("Label '{}'에서 멤버 '{}' 제거 시도", label.getLabelName(), memberIdToRemove);
            label.removeMember(memberToRemove);
        }
        labelRepository.saveAll(labels);
    }
}
