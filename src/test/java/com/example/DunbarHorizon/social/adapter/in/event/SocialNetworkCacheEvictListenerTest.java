package com.example.DunbarHorizon.social.adapter.in.event;

import com.example.DunbarHorizon.social.application.port.out.SocialNetworkCacheManager;
import com.example.DunbarHorizon.social.domain.friend.event.FriendRequestAcceptedEvent;
import com.example.DunbarHorizon.social.domain.friend.event.FriendShipDeletedEvent;
import com.example.DunbarHorizon.social.domain.label.event.LabelMemberChangedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SocialNetworkCacheEvictListenerTest {

    @Mock
    private SocialNetworkCacheManager cacheManager;

    @InjectMocks
    private SocialNetworkCacheEvictListener listener;

    @Test
    @DisplayName("친구 수락 이벤트: 요청자·수신자 양측의 Default 네트워크 캐시를 무효화한다")
    void 친구_수락_이벤트_발생_시_양측_Default_네트워크를_무효화한다() {
        // given
        FriendRequestAcceptedEvent event = new FriendRequestAcceptedEvent("req-1", 10L, 20L, "수신자닉네임");

        // when
        listener.onFriendRequestAccepted(event);

        // then
        verify(cacheManager).evictDefaultNetwork(10L);
        verify(cacheManager).evictDefaultNetwork(20L);
        verifyNoMoreInteractions(cacheManager);
    }

    @Test
    @DisplayName("친구 수락 이벤트: 라벨 네트워크 캐시는 무효화하지 않는다")
    void 친구_수락_이벤트_발생_시_라벨_캐시는_건드리지_않는다() {
        // given
        FriendRequestAcceptedEvent event = new FriendRequestAcceptedEvent("req-1", 10L, 20L, "닉네임");

        // when
        listener.onFriendRequestAccepted(event);

        // then
        verify(cacheManager, never()).evictLabelNetwork(anyLong(), anyString());
        verify(cacheManager, never()).evictAllLabelNetworks(anyLong());
    }

    @Test
    @DisplayName("친구 삭제 이벤트: 양측의 Default 네트워크와 전체 라벨 캐시를 무효화한다")
    void 친구_삭제_이벤트_발생_시_양측_Default_및_전체_라벨_캐시를_무효화한다() {
        // given
        FriendShipDeletedEvent event = new FriendShipDeletedEvent(30L, 40L);

        // when
        listener.onFriendShipDeleted(event);

        // then
        verify(cacheManager).evictDefaultNetwork(30L);
        verify(cacheManager).evictDefaultNetwork(40L);
        verify(cacheManager).evictAllLabelNetworks(30L);
        verify(cacheManager).evictAllLabelNetworks(40L);
        verifyNoMoreInteractions(cacheManager);
    }

    @Test
    @DisplayName("라벨 멤버 변경 이벤트: 해당 라벨 캐시만 무효화하고 Default 네트워크는 건드리지 않는다")
    void 라벨_멤버_변경_이벤트_발생_시_해당_라벨_캐시만_무효화한다() {
        // given
        LabelMemberChangedEvent event = new LabelMemberChangedEvent(50L, "label-xyz");

        // when
        listener.onLabelMemberChanged(event);

        // then
        verify(cacheManager).evictLabelNetwork(50L, "label-xyz");
        verifyNoMoreInteractions(cacheManager);
    }

    @Test
    @DisplayName("라벨 멤버 변경 이벤트: 다른 라벨의 캐시나 Default 네트워크는 무효화하지 않는다")
    void 라벨_멤버_변경_이벤트_발생_시_다른_캐시는_건드리지_않는다() {
        // given
        LabelMemberChangedEvent event = new LabelMemberChangedEvent(50L, "label-xyz");

        // when
        listener.onLabelMemberChanged(event);

        // then
        verify(cacheManager, never()).evictDefaultNetwork(anyLong());
        verify(cacheManager, never()).evictAllLabelNetworks(anyLong());
    }
}
