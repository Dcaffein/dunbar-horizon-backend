package com.example.DunbarHorizon.social.application.label;

import com.example.DunbarHorizon.social.domain.friend.event.FriendshipEndedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LabelMembershipEventListener {

    private final LabelCommandService labelCommandService;

    @EventListener
    public void onFriendshipEnded(FriendshipEndedEvent event) {
        labelCommandService.removeMembershipsBetween(event.firstUserId(), event.secondUserId());
    }
}
