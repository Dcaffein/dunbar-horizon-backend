package com.example.DunbarHorizon.social.application.eventListener;

import com.example.DunbarHorizon.social.application.port.out.SocialNetworkCacheManager;
import com.example.DunbarHorizon.social.domain.friend.event.FriendshipCreatedEvent;
import com.example.DunbarHorizon.social.domain.friend.event.FriendShipDeletedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SocialNetworkCacheEvictListenerTest {

    @Mock
    private SocialNetworkCacheManager cacheManager;

    @InjectMocks
    private SocialNetworkCacheEvictListener listener;

    @Test
    @DisplayName("친구 생성 이벤트: 양측의 Default 네트워크 캐시를 무효화한다")
    void 친구_생성_이벤트_발생_시_양측_Default_네트워크를_무효화한다() {
        // given
        FriendshipCreatedEvent event = new FriendshipCreatedEvent(10L, 20L);

        // when
        listener.onFriendshipCreated(event);

        // then
        verify(cacheManager).evictDefaultNetwork(10L);
        verify(cacheManager).evictDefaultNetwork(20L);
        verifyNoMoreInteractions(cacheManager);
    }

    @Test
    @DisplayName("친구 삭제 이벤트: 양측의 Default 네트워크 캐시를 무효화한다")
    void 친구_삭제_이벤트_발생_시_양측_Default_네트워크를_무효화한다() {
        // given
        FriendShipDeletedEvent event = new FriendShipDeletedEvent(30L, 40L);

        // when
        listener.onFriendShipDeleted(event);

        // then
        verify(cacheManager).evictDefaultNetwork(30L);
        verify(cacheManager).evictDefaultNetwork(40L);
        verifyNoMoreInteractions(cacheManager);
    }
}
