package com.example.DunbarHorizon.social.application;

import com.example.DunbarHorizon.social.application.eventHandler.LabelMemberShipEventListener;
import com.example.DunbarHorizon.social.domain.friend.event.FriendShipDeletedEvent;
import com.example.DunbarHorizon.social.domain.label.Label;
import com.example.DunbarHorizon.social.domain.label.repository.LabelRepository;
import com.example.DunbarHorizon.social.domain.socialUser.SocialUser;
import com.example.DunbarHorizon.social.domain.socialUser.repository.SocialUserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LabelMemberShipEventListenerTest {

    @InjectMocks
    private LabelMemberShipEventListener labelMemberShipEventListener;
    @Mock
    private LabelRepository labelRepository;
    @Mock private SocialUserRepository socialUserRepository;

    @Test
    @DisplayName("친구 삭제 이벤트 발생 시 양쪽 사용자의 라벨에서 서로를 제거한다")
    void handleFriendShipDeleted_Success() {
        // given
        Long userA_Id = 1L;
        Long userB_Id = 2L;
        FriendShipDeletedEvent event = new FriendShipDeletedEvent(userA_Id, userB_Id);

        SocialUser userA = new SocialUser(userA_Id, "userA", null);
        SocialUser userB = new SocialUser(userB_Id, "userB", null);

        // UserA의 라벨들 준비
        Label labelA1 = mock(Label.class);
        given(labelA1.getLabelName()).willReturn("라벨A1");
        given(socialUserRepository.findById(userA_Id)).willReturn(Optional.of(userA));
        given(socialUserRepository.findById(userB_Id)).willReturn(Optional.of(userB));
        given(labelRepository.findAllByOwner_Id(userA_Id)).willReturn(List.of(labelA1));

        // UserB의 라벨들 준비
        Label labelB1 = mock(Label.class);
        given(labelB1.getLabelName()).willReturn("라벨B1");
        given(labelRepository.findAllByOwner_Id(userB_Id)).willReturn(List.of(labelB1));

        // when
        labelMemberShipEventListener.handleFriendShipDeleted(event);

        // then
        // 1. UserA의 라벨A1에서 UserB가 제거되었는지 확인
        verify(labelA1).removeMember(userB);

        // 2. UserB의 라벨B1에서 UserA가 제거되었는지 확인
        verify(labelB1).removeMember(userA);
    }
}