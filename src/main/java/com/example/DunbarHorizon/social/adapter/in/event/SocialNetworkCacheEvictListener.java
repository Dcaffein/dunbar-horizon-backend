package com.example.DunbarHorizon.social.adapter.in.event;

import com.example.DunbarHorizon.social.application.port.out.SocialNetworkCacheManager;
import com.example.DunbarHorizon.social.domain.friend.event.FriendRequestAcceptedEvent;
import com.example.DunbarHorizon.social.domain.friend.event.FriendShipDeletedEvent;
import com.example.DunbarHorizon.social.domain.label.event.LabelMemberChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class SocialNetworkCacheEvictListener {

    private final SocialNetworkCacheManager cacheManager;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFriendRequestAccepted(FriendRequestAcceptedEvent event) {
        cacheManager.evictDefaultNetwork(event.requesterId());
        cacheManager.evictDefaultNetwork(event.receiverId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFriendShipDeleted(FriendShipDeletedEvent event) {
        cacheManager.evictDefaultNetwork(event.userAId());
        cacheManager.evictDefaultNetwork(event.userBId());
        cacheManager.evictAllLabelNetworks(event.userAId());
        cacheManager.evictAllLabelNetworks(event.userBId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLabelMemberChanged(LabelMemberChangedEvent event) {
        cacheManager.evictLabelNetwork(event.ownerId(), event.labelId());
    }
}
