package com.example.DunbarHorizon.social.application.service;

import com.example.DunbarHorizon.social.application.port.out.ActiveUserLookup;
import com.example.DunbarHorizon.social.application.port.out.LabelMemberFriendshipLookup;
import com.example.DunbarHorizon.social.application.port.out.LabelMembershipCleanup;
import com.example.DunbarHorizon.social.application.port.out.LabelRepository;
import com.example.DunbarHorizon.social.domain.label.Label;
import com.example.DunbarHorizon.social.domain.label.LabelMemberRegistrar;
import com.example.DunbarHorizon.social.domain.label.LabelNamer;
import com.example.DunbarHorizon.social.domain.label.LabelTestFactory;
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
class LabelCommandServiceTest {

    @Mock private LabelRepository labelRepository;
    @Mock private LabelMembershipCleanup labelMembershipCleanup;
    @Mock private LabelMemberFriendshipLookup labelMemberFriendshipLookup;
    @Mock private ActiveUserLookup activeUserLookup;

    private LabelCommandService service;
    private final Label label = LabelTestFactory.createLabel(1L, "친구");

    @BeforeEach
    void setUp() {
        var registrar = new LabelMemberRegistrar();
        registrar.addMember(label, 2L, Set.of(2L));
        service = new LabelCommandService(labelRepository, labelMembershipCleanup, new LabelNamer(), registrar,
                labelMemberFriendshipLookup, activeUserLookup);
    }

    @Test
    @DisplayName("소유자의 기존 이름을 사용해 빈 번호를 선택하고 저장한다")
    void createLabel_SavesAvailableName_WhenRequestedNameIsOccupied() {
        // given
        given(activeUserLookup.isActive(1L)).willReturn(true);
        given(labelRepository.findNamesByOwner(1L, null)).willReturn(Set.of("친구", "친구 (3)"));

        // when
        Label created = service.createLabel(1L, "친구");

        // then
        assertThat(created.getLabelName()).isEqualTo("친구 (2)");
        verify(labelRepository).save(created);
    }

    @Test
    @DisplayName("활성 owner가 없으면 이름 중복 조회와 라벨 생성을 수행하지 않는다")
    void createLabel_Rejects_InactiveOwner() {
        // when & then
        assertThatThrownBy(() -> service.createLabel(1L, "친구"))
                .isInstanceOf(UserReferenceNotFoundException.class);
        verify(activeUserLookup).isActive(1L);
        verifyNoInteractions(labelRepository);
    }

    @Test
    @DisplayName("이름 변경은 자기 라벨을 제외한 중복 사실을 사용한다")
    void updateLabel_Succeeds_SameName() {
        given(labelRepository.findById(label.getId())).willReturn(Optional.of(label));
        given(labelRepository.findNamesByOwner(1L, label.getId())).willReturn(Set.of("친구 (2)"));

        service.updateLabel(label.getId(), 1L, "친구");

        assertThat(label.getLabelName()).isEqualTo("친구");
        verify(labelRepository).save(label);
    }

    @Test
    @DisplayName("이름이 없는 PATCH는 소유권만 확인하고 변경하지 않는다")
    void updateLabel_SkipsRename_NullName() {
        given(labelRepository.findById(label.getId())).willReturn(Optional.of(label));

        service.updateLabel(label.getId(), 1L, null);

        verify(labelRepository, never()).save(any());
        verify(labelRepository, never()).findNamesByOwner(any(), any());
    }

    @Test
    @DisplayName("다른 소유자의 멤버 추가는 사용자와 친구 관계를 조회하기 전에 거부한다")
    void addMember_Rejects_ForeignOwner() {
        Label foreignLabel = LabelTestFactory.createLabel(2L, "친구");
        given(labelRepository.findById(foreignLabel.getId())).willReturn(Optional.of(foreignLabel));

        assertThatThrownBy(() -> service.addMemberToLabel(1L, foreignLabel.getId(), 3L))
                .isInstanceOf(LabelAuthorizationException.class);
        verifyNoInteractions(activeUserLookup, labelMemberFriendshipLookup);
    }

    @Test
    @DisplayName("비활성 대상 추가는 친구 관계 조회 전에 사용자 없음으로 거부한다")
    void addMember_Rejects_InactiveUser() {
        given(labelRepository.findById(label.getId())).willReturn(Optional.of(label));

        assertThatThrownBy(() -> service.addMemberToLabel(1L, label.getId(), 3L))
                .isInstanceOf(UserReferenceNotFoundException.class);
        verify(activeUserLookup).isActive(3L);
        verifyNoInteractions(labelMemberFriendshipLookup);
        verify(labelRepository, never()).save(any());
    }

    @Test
    @DisplayName("교체 대상의 실제 친구 ID를 전달하고 비친구면 기존 멤버를 저장하지 않는다")
    void replaceMembers_Rejects_NonFriend() {
        given(labelMemberFriendshipLookup.findFriendIdsWithSharedLock(1L, List.of(3L))).willReturn(Set.of());
        given(labelRepository.findById(label.getId())).willReturn(Optional.of(label));

        assertThatThrownBy(() -> service.replaceLabelMembers(1L, label.getId(), List.of(3L, 3L)))
                .isInstanceOf(NonFriendLabelMemberException.class);
        assertThat(label.getMemberIds()).containsExactly(2L);
        verify(labelRepository, never()).save(any());
    }

    @Test
    @DisplayName("null 교체 목록은 빈 목록으로 변환해 기존 멤버를 비운다")
    void replaceMembers_Clears_NullInput() {
        given(labelMemberFriendshipLookup.findFriendIdsWithSharedLock(1L, List.of())).willReturn(Set.of());
        given(labelRepository.findById(label.getId())).willReturn(Optional.of(label));

        service.replaceLabelMembers(1L, label.getId(), null);

        assertThat(label.getMemberIds()).isEmpty();
        verify(labelRepository).save(label);
    }
}
