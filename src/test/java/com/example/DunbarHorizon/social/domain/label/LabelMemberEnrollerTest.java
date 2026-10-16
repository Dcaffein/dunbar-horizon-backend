package com.example.DunbarHorizon.social.domain.label;

import com.example.DunbarHorizon.social.domain.friend.exception.FriendshipNotFoundException;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendshipRepository;
import com.example.DunbarHorizon.social.domain.label.exception.DuplicateLabelMemberException;
import com.example.DunbarHorizon.social.domain.label.exception.NonFriendLabelMemberException;
import com.example.DunbarHorizon.social.domain.label.repository.LabelRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class LabelMemberEnrollerTest {

    @InjectMocks
    private LabelMemberEnroller labelMemberEnroller;

    @Mock
    private FriendshipRepository friendshipRepository;

    @Mock
    private LabelRepository labelRepository;

    private final String labelId = "label-1";
    private final Long ownerId = 1L;
    private final Long friendId = 2L;

    @Test
    @DisplayName("친구가 아닌 사용자를 라벨에 추가하려 하면 예외가 발생하고 멤버를 추가하지 않는다")
    void addNewMember_Fail_NotFriend() {
        // given
        given(friendshipRepository.existsFriendshipBetween(ownerId, friendId)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> labelMemberEnroller.addNewMember(labelId, ownerId, friendId))
                .isInstanceOf(FriendshipNotFoundException.class);
        verify(labelRepository, never()).addMember(any(), any());
    }

    @Test
    @DisplayName("이미 라벨에 존재하는 멤버를 추가하려 하면 예외가 발생하고 멤버를 추가하지 않는다")
    void addNewMember_Fail_Duplicated() {
        // given
        given(friendshipRepository.existsFriendshipBetween(ownerId, friendId)).willReturn(true);
        given(labelRepository.existsMember(labelId, friendId)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> labelMemberEnroller.addNewMember(labelId, ownerId, friendId))
                .isInstanceOf(DuplicateLabelMemberException.class);
        verify(labelRepository, never()).addMember(any(), any());
    }

    @Test
    @DisplayName("친구이고 아직 멤버가 아니면 라벨에 멤버를 추가한다")
    void addNewMember_Success() {
        // given
        given(friendshipRepository.existsFriendshipBetween(ownerId, friendId)).willReturn(true);
        given(labelRepository.existsMember(labelId, friendId)).willReturn(false);

        // when
        labelMemberEnroller.addNewMember(labelId, ownerId, friendId);

        // then
        verify(labelRepository).addMember(labelId, friendId);
    }

    @Test
    @DisplayName("친구가 아닌 ID가 포함된 경우 일괄 교체 시 예외가 발생하고 멤버를 교체하지 않는다")
    void updateMembers_Fail_ContainsNonFriendIds() {
        // given
        List<Long> requestedIds = List.of(2L, 3L);
        given(friendshipRepository.filterFriendIdsAmong(ownerId, requestedIds)).willReturn(Set.of(2L));

        // when & then
        assertThatThrownBy(() -> labelMemberEnroller.updateMembers(labelId, ownerId, requestedIds))
                .isInstanceOf(NonFriendLabelMemberException.class)
                .hasMessageContaining("3");
        verify(labelRepository, never()).replaceMembers(any(), any());
    }

    @Test
    @DisplayName("모든 ID가 친구라면 중복을 제거한 ID로 라벨 멤버를 교체한다")
    void updateMembers_Success() {
        // given
        List<Long> requestedIds = List.of(2L, 2L, 3L);
        given(friendshipRepository.filterFriendIdsAmong(ownerId, List.of(2L, 3L))).willReturn(Set.of(2L, 3L));

        // when
        labelMemberEnroller.updateMembers(labelId, ownerId, requestedIds);

        // then
        verify(labelRepository).replaceMembers(labelId, Set.of(2L, 3L));
    }

    @Test
    @DisplayName("빈 리스트로 멤버 교체 시 친구 검증 없이 모든 멤버를 제거한다")
    void updateMembers_EmptyList_ClearsMembers() {
        // when
        labelMemberEnroller.updateMembers(labelId, ownerId, List.of());

        // then
        verify(labelRepository).replaceMembers(labelId, Set.of());
        verifyNoInteractions(friendshipRepository);
    }
}
