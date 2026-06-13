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

    @Test
    @DisplayName("한쪽만 상대방을 라벨에 등록한 경우 해당 쪽만 saveAll이 호출된다")
    void handleFriendShipDeleted_한쪽만_라벨_보유_시_단방향_저장() {
        // given
        Long userAId = 1L;
        Long userBId = 2L;
        FriendShipDeletedEvent event = new FriendShipDeletedEvent(userAId, userBId);

        SocialUser userA = new SocialUser(userAId, "userA", null);
        SocialUser userB = new SocialUser(userBId, "userB", null);

        // A는 B가 포함된 라벨 1개 보유
        Label labelA = mock(Label.class);
        given(labelA.getMembers()).willReturn(new HashSet<>(Set.of(userB)));
        given(labelRepository.findLabelsByOwnerAndMember(userAId, userBId)).willReturn(List.of(labelA));

        // B는 A가 포함된 라벨 없음
        given(labelRepository.findLabelsByOwnerAndMember(userBId, userAId)).willReturn(List.of());

        // when
        labelMemberShipEventListener.handleFriendShipDeleted(event);

        // then
        verify(labelA).removeMember(userB);
        verify(labelRepository, times(1)).saveAll(any()); // A 쪽만 saveAll 호출
    }

    @Test
    @DisplayName("여러 라벨에 해당 멤버가 포함된 경우 모든 라벨에서 제거하고 한 번에 saveAll을 호출한다")
    void handleFriendShipDeleted_다중_라벨에서_멤버_일괄_제거() {
        // given
        Long userAId = 1L;
        Long userBId = 2L;
        FriendShipDeletedEvent event = new FriendShipDeletedEvent(userAId, userBId);

        SocialUser userB = new SocialUser(userBId, "userB", null);

        // A가 B를 포함한 라벨 2개 보유
        Label label1 = mock(Label.class);
        given(label1.getMembers()).willReturn(new HashSet<>(Set.of(userB)));
        Label label2 = mock(Label.class);
        given(label2.getMembers()).willReturn(new HashSet<>(Set.of(userB)));
        given(labelRepository.findLabelsByOwnerAndMember(userAId, userBId)).willReturn(List.of(label1, label2));

        // B 쪽은 라벨 없음
        given(labelRepository.findLabelsByOwnerAndMember(userBId, userAId)).willReturn(List.of());

        // when
        labelMemberShipEventListener.handleFriendShipDeleted(event);

        // then — 두 라벨 모두 removeMember 호출, saveAll은 한 번
        verify(label1).removeMember(userB);
        verify(label2).removeMember(userB);
        verify(labelRepository, times(1)).saveAll(List.of(label1, label2));
    }
}
