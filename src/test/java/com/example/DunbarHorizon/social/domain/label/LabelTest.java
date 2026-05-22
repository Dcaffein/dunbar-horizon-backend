package com.example.DunbarHorizon.social.domain.label;

import com.example.DunbarHorizon.social.domain.label.exception.InvalidLabelNameException;
import com.example.DunbarHorizon.social.domain.label.exception.LabelAuthorizationException;
import com.example.DunbarHorizon.social.domain.socialUser.SocialUser;
import org.junit.jupiter.api.BeforeEach;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

class LabelTest {

    private SocialUser owner;
    private SocialUser friend;

    @BeforeEach
    void setUp() {
        owner = new SocialUser(1L, "owner", "profile1");
        friend = new SocialUser(2L, "friend", "profile2");
    }

    @Test
    @DisplayName("라벨 생성 시 초기 멤버는 비어있어야 한다")
    void createLabel_Success() {
        // when
        Label label = new Label(owner, "친구들", true);

        // then
        assertThat(label.getLabelName()).isEqualTo("친구들");
        assertThat(label.getOwner().getId()).isEqualTo(1L);
        assertThat(label.isExposure()).isTrue();
        assertThat(label.getMembers()).isEmpty();
    }

    @Test
    @DisplayName("라벨에 새로운 멤버를 추가할 수 있다")
    void addNewMember_Success() {
        // given
        Label label = new Label(owner, "친구들", true);

        // when
        label.addNewMember(friend);

        // then
        assertThat(label.getMembers()).hasSize(1);
        assertThat(label.getMembers()).contains(friend);
    }

    @Test
    @DisplayName("라벨 이름을 변경할 때 빈 값이 들어오면 예외가 발생한다")
    void applyNewLabelName_Fail_BlankName() {
        // given
        Label label = new Label(owner, "친구들", true);

        // when & then
        assertThatThrownBy(() -> label.applyNewLabelName(""))
                .isInstanceOf(InvalidLabelNameException.class);
    }

    @Test
    @DisplayName("소유자가 아닌 유저가 노출 설정을 변경하려 하면 예외가 발생한다")
    void updateExposure_Fail_Unauthorized() {
        // given
        Label label = new Label(owner, "친구들", true);
        Long otherUserId = 999L;

        // when & then
        assertThatThrownBy(() -> label.updateExposure(otherUserId, false))
                .isInstanceOf(LabelAuthorizationException.class);
    }

    @Test
    @DisplayName("소유자는 노출 설정을 성공적으로 변경할 수 있다")
    void updateExposure_Success() {
        // given
        Label label = new Label(owner, "친구들", true);

        // when
        label.updateExposure(owner.getId(), false);

        // then
        assertThat(label.isExposure()).isFalse();
    }

    @Test
    @DisplayName("멤버 전체 교체 시 기존 멤버가 제거되고 새 멤버로 대체된다")
    void updateMembers_ReplacesExistingMembers() {
        // given
        Label label = new Label(owner, "친구들", true);
        SocialUser newFriend = new SocialUser(3L, "newFriend", "profile3");
        label.addNewMember(friend);

        // when
        label.updateMembers(Set.of(newFriend));

        // then
        assertThat(label.getMembers()).hasSize(1);
        assertThat(label.getMembers()).contains(newFriend);
        assertThat(label.getMembers()).doesNotContain(friend);
    }

    @Test
    @DisplayName("빈 Set으로 교체 시 모든 멤버가 제거된다")
    void updateMembers_EmptySet_ClearsAll() {
        // given
        Label label = new Label(owner, "친구들", true);
        label.addNewMember(friend);

        // when
        label.updateMembers(Set.of());

        // then
        assertThat(label.getMembers()).isEmpty();
    }
}