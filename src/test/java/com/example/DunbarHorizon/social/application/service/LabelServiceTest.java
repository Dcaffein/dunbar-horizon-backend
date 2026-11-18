package com.example.DunbarHorizon.social.application.service;

import com.example.DunbarHorizon.social.application.dto.UserProfile;
import com.example.DunbarHorizon.social.application.port.out.FriendshipLockRepository;
import com.example.DunbarHorizon.social.application.port.out.LabelQueryRepository;
import com.example.DunbarHorizon.social.application.port.out.LabelRepository;
import com.example.DunbarHorizon.social.application.port.out.UserProfileRepository;
import com.example.DunbarHorizon.social.domain.label.Label;
import com.example.DunbarHorizon.social.domain.label.LabelMemberRegistrar;
import com.example.DunbarHorizon.social.domain.label.LabelNamer;
import com.example.DunbarHorizon.social.domain.label.exception.DuplicateLabelNameException;
import com.example.DunbarHorizon.social.domain.label.exception.LabelAuthorizationException;
import com.example.DunbarHorizon.social.domain.label.exception.NonFriendLabelMemberException;
import com.example.DunbarHorizon.social.domain.socialUser.exception.UserReferenceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class LabelServiceTest {

    @Mock private LabelRepository labelRepository;
    @Mock private LabelQueryRepository labelQueryRepository;
    @Mock private FriendshipLockRepository friendshipLockRepository;
    @Mock private UserProfileRepository userProfileRepository;

    private LabelService service;
    private final Label label = Label.restore("label", "친구", 1L, Set.of(2L));

    @BeforeEach
    void setUp() {
        service = new LabelService(labelRepository, labelQueryRepository, new LabelNamer(), new LabelMemberRegistrar(), friendshipLockRepository, userProfileRepository);
    }

    @Test
    @DisplayName("이름 중복 사실을 도메인에 전달하고 중복이면 저장하지 않는다")
    void createLabel_Rejects_DuplicateName() {
        given(userProfileRepository.findActiveProfiles(List.of(1L))).willReturn(Map.of(1L, new UserProfile(1L, "owner", null)));
        given(labelRepository.existsByOwnerAndName(1L, "친구")).willReturn(true);

        assertThatThrownBy(() -> service.createLabel(1L, "친구")).isInstanceOf(DuplicateLabelNameException.class);
        verify(labelRepository, never()).create(any());
    }

    @Test
    @DisplayName("이름 변경은 자기 라벨을 제외한 중복 사실을 사용한다")
    void updateLabel_Succeeds_SameName() {
        given(labelRepository.lockOwnerId("label")).willReturn(Optional.of(1L));
        given(labelRepository.findById("label")).willReturn(Optional.of(label));

        service.updateLabel("label", 1L, "친구");

        verify(labelRepository).existsByOwnerAndNameExcludingLabel(1L, "친구", "label");
        verify(labelRepository).saveChanges(label);
    }

    @Test
    @DisplayName("이름이 없는 PATCH는 소유권만 확인하고 변경하지 않는다")
    void updateLabel_SkipsRename_NullName() {
        given(labelRepository.lockOwnerId("label")).willReturn(Optional.of(1L));
        given(labelRepository.findById("label")).willReturn(Optional.of(label));

        service.updateLabel("label", 1L, null);

        verify(labelRepository, never()).saveChanges(any());
        verify(labelRepository, never()).existsByOwnerAndNameExcludingLabel(any(), any(), any());
    }

    @Test
    @DisplayName("다른 소유자의 멤버 추가는 사용자와 친구 관계를 조회하기 전에 거부한다")
    void addMember_Rejects_ForeignOwner() {
        given(labelRepository.findById("label"))
                .willReturn(Optional.of(Label.restore("label", "친구", 2L, Set.of())));

        assertThatThrownBy(() -> service.addMemberToLabel(1L, "label", 3L))
                .isInstanceOf(LabelAuthorizationException.class);
        verifyNoInteractions(userProfileRepository, friendshipLockRepository);
    }

    @Test
    @DisplayName("비활성 대상 추가는 친구 관계 조회 전에 사용자 없음으로 거부한다")
    void addMember_Rejects_InactiveUser() {
        given(labelRepository.findById("label")).willReturn(Optional.of(label));

        assertThatThrownBy(() -> service.addMemberToLabel(1L, "label", 3L))
                .isInstanceOf(UserReferenceNotFoundException.class);
        verifyNoInteractions(friendshipLockRepository);
        verify(labelRepository, never()).saveChanges(any());
    }

    @Test
    @DisplayName("교체 대상의 실제 친구 ID를 전달하고 비친구면 기존 멤버를 저장하지 않는다")
    void replaceMembers_Rejects_NonFriend() {
        given(friendshipLockRepository.findAllBetweenForUpdate(1L, List.of(3L))).willReturn(List.of());
        given(labelRepository.lockOwnerId("label")).willReturn(Optional.of(1L));
        given(labelRepository.findById("label")).willReturn(Optional.of(label));

        assertThatThrownBy(() -> service.replaceLabelMembers(1L, "label", List.of(3L, 3L)))
                .isInstanceOf(NonFriendLabelMemberException.class);
        assertThat(label.getMemberIds()).containsExactly(2L);
        verify(labelRepository, never()).saveChanges(any());
    }

    @Test
    @DisplayName("null 교체 목록은 빈 목록으로 변환해 기존 멤버를 비운다")
    void replaceMembers_Clears_NullInput() {
        given(friendshipLockRepository.findAllBetweenForUpdate(1L, List.of())).willReturn(List.of());
        given(labelRepository.lockOwnerId("label")).willReturn(Optional.of(1L));
        given(labelRepository.findById("label")).willReturn(Optional.of(label));

        service.replaceLabelMembers(1L, "label", null);

        assertThat(label.getMemberIds()).isEmpty();
        verify(labelRepository).saveChanges(label);
    }
}
