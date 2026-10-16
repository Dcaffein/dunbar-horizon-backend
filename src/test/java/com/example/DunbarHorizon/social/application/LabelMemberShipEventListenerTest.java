package com.example.DunbarHorizon.social.application;

import com.example.DunbarHorizon.social.application.eventListener.LabelMemberShipEventListener;
import com.example.DunbarHorizon.social.domain.friend.event.FriendShipDeletedEvent;
import com.example.DunbarHorizon.social.domain.label.repository.LabelRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class LabelMemberShipEventListenerTest {

    @InjectMocks
    private LabelMemberShipEventListener labelMemberShipEventListener;
    @Mock
    private LabelRepository labelRepository;

    @Test
    @DisplayName("친구 삭제 이벤트 발생 시 양쪽 소유 라벨에서 상대방을 제거한다")
    void handleFriendShipDeleted_양쪽_라벨에서_상대방_제거() {
        // given
        Long userAId = 1L;
        Long userBId = 2L;
        FriendShipDeletedEvent event = new FriendShipDeletedEvent(userAId, userBId);

        // when
        labelMemberShipEventListener.handleFriendShipDeleted(event);

        // then
        verify(labelRepository).removeMemberFromOwnedLabels(userAId, userBId);
        verify(labelRepository).removeMemberFromOwnedLabels(userBId, userAId);
        verifyNoMoreInteractions(labelRepository);
    }
}
