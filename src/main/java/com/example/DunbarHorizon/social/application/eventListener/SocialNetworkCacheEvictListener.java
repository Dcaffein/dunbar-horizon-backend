package com.example.DunbarHorizon.social.application.eventListener;

import com.example.DunbarHorizon.social.application.port.out.SocialNetworkCacheManager;
import com.example.DunbarHorizon.social.domain.friend.event.FriendShipDeletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class SocialNetworkCacheEvictListener {

    private final SocialNetworkCacheManager cacheManager;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFriendShipDeleted(FriendShipDeletedEvent event) {
        cacheManager.evictDefaultNetwork(event.userAId());
        cacheManager.evictDefaultNetwork(event.userBId());
    }
}
