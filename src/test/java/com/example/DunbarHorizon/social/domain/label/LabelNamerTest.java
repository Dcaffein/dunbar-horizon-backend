package com.example.DunbarHorizon.social.domain.label;

import com.example.DunbarHorizon.social.domain.label.exception.DuplicateLabelNameException;
import com.example.DunbarHorizon.social.domain.label.exception.InvalidLabelNameException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LabelNamerTest {

    private final LabelNamer labelNamer = new LabelNamer();

    @Test
    @DisplayName("소유자에게 중복 이름이 없으면 라벨을 생성한다")
    void create_Succeeds_UniqueName() {
        Label label = labelNamer.create(1L, "친구", false);

        assertThat(label.getOwnerId()).isEqualTo(1L);
        assertThat(label.getLabelName()).isEqualTo("친구");
        assertThat(label.getMemberIds()).isEmpty();
    }

    @Test
    @DisplayName("같은 소유자의 이름 중복 사실이 있으면 생성을 거부한다")
    void create_Rejects_DuplicateName() {
        assertThatThrownBy(() -> labelNamer.create(1L, "친구", true))
                .isInstanceOf(DuplicateLabelNameException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "123456789012345678901"})
    @DisplayName("생성 이름의 공백과 길이 규칙을 모델이 검증한다")
    void create_Rejects_InvalidName(String name) {
        assertThatThrownBy(() -> labelNamer.create(1L, name, false))
                .isInstanceOf(InvalidLabelNameException.class);
    }

    @Test
    @DisplayName("자기 이름 재지정과 중복 없는 이름 변경을 허용한다")
    void rename_Succeeds_SameOrUniqueName() {
        // given
        Label label = labelNamer.create(1L, "친구", false);

        // when
        labelNamer.rename(label, "친구", false);
        labelNamer.rename(label, "동료", false);

        // then
        assertThat(label.getLabelName()).isEqualTo("동료");
    }

    @Test
    @DisplayName("다른 라벨이 이름을 사용하면 기존 이름을 유지하고 실패한다")
    void rename_Rejects_DuplicateName() {
        Label label = labelNamer.create(1L, "친구", false);

        assertThatThrownBy(() -> labelNamer.rename(label, "동료", true))
                .isInstanceOf(DuplicateLabelNameException.class);
        assertThat(label.getLabelName()).isEqualTo("친구");
    }

    @Test
    @DisplayName("잘못된 이름으로 변경하면 기존 이름을 유지한다")
    void rename_Rejects_InvalidName() {
        Label label = labelNamer.create(1L, "친구", false);

        assertThatThrownBy(() -> labelNamer.rename(label, " ", false))
                .isInstanceOf(InvalidLabelNameException.class);
        assertThat(label.getLabelName()).isEqualTo("친구");
    }
}
