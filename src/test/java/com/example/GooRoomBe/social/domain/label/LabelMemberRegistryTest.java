package com.example.GooRoomBe.social.domain.label;

import com.example.GooRoomBe.social.domain.friend.exception.FriendshipNotFoundException;
import com.example.GooRoomBe.social.domain.friend.repository.FriendshipRepository;
import com.example.GooRoomBe.social.domain.label.exception.DuplicateLabelMemberException;
import com.example.GooRoomBe.social.domain.label.exception.NonFriendLabelMemberException;
import com.example.GooRoomBe.social.domain.socialUser.SocialUser;
import com.example.GooRoomBe.social.domain.socialUser.SocialUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class LabelMemberRegistryTest {

    @InjectMocks
    private LabelMemberRegistry labelMemberRegistry;

    @Mock
    private FriendshipRepository friendshipRepository;

    @Mock
    private SocialUserRepository socialUserRepository;

    private Label label;
    private SocialUser owner;
    private SocialUser friend;

    @BeforeEach
    void setUp() {
        owner = new SocialUser(1L, "owner", null);
        friend = new SocialUser(2L, "friend", null);
        label = new Label(owner, "친구들", true);
    }

    @Test
    @DisplayName("친구가 아닌 사용자를 라벨에 추가하려 하면 예외가 발생한다")
    void addNewMember_Fail_NotFriend() {
        // given
        given(friendshipRepository.existsFriendshipBetween(owner.getId(), friend.getId())).willReturn(false);

        // when & then
        assertThatThrownBy(() -> labelMemberRegistry.addNewMember(label, friend))
                .isInstanceOf(FriendshipNotFoundException.class);
    }

    @Test
    @DisplayName("이미 라벨에 존재하는 멤버를 추가하려 하면 예외가 발생한다")
    void addNewMember_Fail_Duplicated() {
        // given
        label.addNewMember(friend);
        given(friendshipRepository.existsFriendshipBetween(owner.getId(), friend.getId())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> labelMemberRegistry.addNewMember(label, friend))
                .isInstanceOf(DuplicateLabelMemberException.class);
    }

    @Test
    @DisplayName("친구가 아닌 ID가 포함된 경우 일괄 교체 시 예외가 발생한다")
    void updateMembers_Fail_ContainsNonFriendIds() {
        // given
        List<Long> requestedIds = List.of(2L, 3L);
        given(friendshipRepository.findFriendIdsIn(owner.getId(), requestedIds)).willReturn(Set.of(2L));

        // when & then
        assertThatThrownBy(() -> labelMemberRegistry.updateMembers(label, requestedIds))
                .isInstanceOf(NonFriendLabelMemberException.class)
                .hasMessageContaining("3");
    }

    @Test
    @DisplayName("모든 ID가 친구라면 라벨 멤버 일괄 교체에 성공한다")
    void updateMembers_Success() {
        // given
        List<Long> requestedIds = List.of(2L);
        given(friendshipRepository.findFriendIdsIn(owner.getId(), requestedIds)).willReturn(Set.of(2L));
        given(socialUserRepository.findAllUserReferencesById(Set.of(2L))).willReturn(Set.of(friend));

        // when
        labelMemberRegistry.updateMembers(label, requestedIds);

        // then
        assertThat(label.getMembers()).hasSize(1).contains(friend);
    }

    @Test
    @DisplayName("빈 리스트로 멤버 업데이트 시 모든 멤버가 제거된다")
    void updateMembers_EmptyList_ClearsMembers() {
        // given
        label.addNewMember(friend);

        // when
        labelMemberRegistry.updateMembers(label, List.of());

        // then
        assertThat(label.getMembers()).isEmpty();
    }
}
