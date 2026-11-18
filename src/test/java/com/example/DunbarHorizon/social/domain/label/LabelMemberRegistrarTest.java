package com.example.DunbarHorizon.social.domain.label;

import com.example.DunbarHorizon.social.domain.friendship.exception.FriendshipNotFoundException;
import com.example.DunbarHorizon.social.domain.label.exception.DuplicateLabelMemberException;
import com.example.DunbarHorizon.social.domain.label.exception.NonFriendLabelMemberException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LabelMemberRegistrarTest {

    private final LabelMemberRegistrar registrar = new LabelMemberRegistrar();
    private final Label label = Label.create(1L, new LabelName("친구"));

    @Test
    @DisplayName("실제 친구 ID에 포함된 멤버를 추가하고 중복 추가는 거부한다")
    void addMember_RejectsDuplicate_ExistingMember() {
        registrar.addMember(label, 2L, Set.of(2L));

        assertThat(label.getMemberIds()).containsExactly(2L);
        assertThatThrownBy(() -> registrar.addMember(label, 2L, Set.of(2L)))
                .isInstanceOf(DuplicateLabelMemberException.class);
    }

    @Test
    @DisplayName("친구가 아닌 단건 추가는 기존 친구 관계 없음 예외로 거부한다")
    void addMember_Rejects_NonFriend() {
        assertThatThrownBy(() -> registrar.addMember(label, 2L, Set.of(3L)))
                .isInstanceOf(FriendshipNotFoundException.class);
        assertThat(label.getMemberIds()).isEmpty();
    }

    @Test
    @DisplayName("친구로만 구성된 요청 목록으로 교체하고 중복 입력을 하나로 저장한다")
    void replaceMembers_Succeeds_DuplicateInput() {
        // given
        registrar.addMember(label, 2L, Set.of(2L));

        // when
        registrar.replaceMembers(label, List.of(3L, 3L), Set.of(3L));

        // then
        assertThat(label.getMemberIds()).containsExactly(3L);
    }

    @Test
    @DisplayName("비친구가 섞인 교체는 비친구 ID를 식별하고 기존 멤버를 유지한다")
    void replaceMembers_Rejects_MixedNonFriends() {
        registrar.addMember(label, 2L, Set.of(2L));

        assertThatThrownBy(() -> registrar.replaceMembers(label, List.of(2L, 3L, 4L, 3L), Set.of(2L)))
                .isInstanceOf(NonFriendLabelMemberException.class)
                .hasMessageContaining("nonFriendIds=[3, 4]");
        assertThat(label.getMemberIds()).containsExactly(2L);
    }

    @Test
    @DisplayName("빈 교체 요청은 모든 멤버를 제거한다")
    void replaceMembers_Clears_EmptyInput() {
        registrar.addMember(label, 2L, Set.of(2L));

        registrar.replaceMembers(label, List.of(), Set.of());

        assertThat(label.getMemberIds()).isEmpty();
    }

    @Test
    @DisplayName("모델에서 멤버를 제거하며 외부에서 멤버 집합을 바꿀 수 없다")
    void removeMember_Succeeds_ExistingOrMissingMember() {
        registrar.addMember(label, 2L, Set.of(2L));
        assertThatThrownBy(() -> label.getMemberIds().clear()).isInstanceOf(UnsupportedOperationException.class);

        label.removeMember(2L);
        label.removeMember(2L);

        assertThat(label.getMemberIds()).isEmpty();
    }
}
