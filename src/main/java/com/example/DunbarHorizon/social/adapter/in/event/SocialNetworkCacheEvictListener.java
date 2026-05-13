package com.example.DunbarHorizon.social.adapter.in.event;

import com.example.DunbarHorizon.social.application.port.out.SocialNetworkCacheRepository;
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

    private final SocialNetworkCacheRepository cacheRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFriendRequestAccepted(FriendRequestAcceptedEvent event) {
        cacheRepository.evictDefaultNetwork(event.requesterId());
        cacheRepository.evictDefaultNetwork(event.receiverId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFriendShipDeleted(FriendShipDeletedEvent event) {
        cacheRepository.evictDefaultNetwork(event.userAId());
        cacheRepository.evictDefaultNetwork(event.userBId());
        cacheRepository.evictAllLabelNetworks(event.userAId());
        cacheRepository.evictAllLabelNetworks(event.userBId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLabelMemberChanged(LabelMemberChangedEvent event) {
        cacheRepository.evictLabelNetwork(event.ownerId(), event.labelId());
    }
}
