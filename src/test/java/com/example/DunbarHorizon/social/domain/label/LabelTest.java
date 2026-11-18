package com.example.DunbarHorizon.social.domain.label;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

class LabelTest {

    private Long owner;

    @BeforeEach
    void setUp() {
        owner = 1L;
    }

    @Test
    @DisplayName("라벨 생성 시 초기 멤버는 비어있어야 한다")
    void createLabel_Success() {
        // when
        Label label = Label.create(owner, new LabelName("친구들"));

        // then
        assertThat(label.getLabelName()).isEqualTo("친구들");
        assertThat(label.getOwnerId()).isEqualTo(1L);
        assertThat(label.getMemberIds()).isEmpty();
    }

    @Test
    @DisplayName("확정된 이름으로 라벨 이름을 변경한다")
    void rename_ChangesName_WithAssignedName() {
        // given
        Label label = Label.create(owner, new LabelName("친구들"));

        // when
        label.rename(new LabelName("동료"));

        // then
        assertThat(label.getLabelName()).isEqualTo("동료");
    }
}
