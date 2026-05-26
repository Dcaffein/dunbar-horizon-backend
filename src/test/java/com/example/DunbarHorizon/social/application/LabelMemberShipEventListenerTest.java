package com.example.DunbarHorizon.social.application;

import com.example.DunbarHorizon.social.application.eventListener.LabelMemberShipEventListener;
import com.example.DunbarHorizon.social.domain.friend.event.FriendShipDeletedEvent;
import com.example.DunbarHorizon.social.domain.label.Label;
import com.example.DunbarHorizon.social.domain.label.repository.LabelRepository;
import com.example.DunbarHorizon.social.domain.socialUser.SocialUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LabelMemberShipEventListenerTest {

    @InjectMocks
    private LabelMemberShipEventListener labelMemberShipEventListener;
    @Mock
    private LabelRepository labelRepository;

    @Test
    @DisplayName("친구 삭제 이벤트 발생 시 해당 멤버가 속한 라벨에서만 멤버를 제거한다")
    void handleFriendShipDeleted_양쪽_라벨에서_상대방_제거() {
        // given
        Long userAId = 1L;
        Long userBId = 2L;
        FriendShipDeletedEvent event = new FriendShipDeletedEvent(userAId, userBId);

        SocialUser userA = new SocialUser(userAId, "userA", null);
        SocialUser userB = new SocialUser(userBId, "userB", null);

        Label labelA = mock(Label.class);
        given(labelA.getMembers()).willReturn(new HashSet<>(Set.of(userB)));
        given(labelRepository.findLabelsByOwnerAndMember(userAId, userBId)).willReturn(List.of(labelA));

        Label labelB = mock(Label.class);
        given(labelB.getMembers()).willReturn(new HashSet<>(Set.of(userA)));
        given(labelRepository.findLabelsByOwnerAndMember(userBId, userAId)).willReturn(List.of(labelB));

        // when
        labelMemberShipEventListener.handleFriendShipDeleted(event);

        // then
        verify(labelA).removeMember(userB);
        verify(labelB).removeMember(userA);
        verify(labelRepository).saveAll(List.of(labelA));
        verify(labelRepository).saveAll(List.of(labelB));
    }

    @Test
    @DisplayName("멤버가 어떤 라벨에도 속하지 않은 경우 saveAll을 호출하지 않는다")
    void handleFriendShipDeleted_라벨_없으면_saveAll_미호출() {
        // given
        Long userAId = 1L;
        Long userBId = 2L;
        FriendShipDeletedEvent event = new FriendShipDeletedEvent(userAId, userBId);

        given(labelRepository.findLabelsByOwnerAndMember(userAId, userBId)).willReturn(List.of());
        given(labelRepository.findLabelsByOwnerAndMember(userBId, userAId)).willReturn(List.of());

        // when
        labelMemberShipEventListener.handleFriendShipDeleted(event);

        // then
        verify(labelRepository, never()).saveAll(any());
    }
}
