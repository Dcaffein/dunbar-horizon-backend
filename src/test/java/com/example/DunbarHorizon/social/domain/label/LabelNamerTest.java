package com.example.DunbarHorizon.social.domain.label;

import com.example.DunbarHorizon.social.domain.label.exception.InvalidLabelNameException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LabelNamerTest {

    private final LabelNamer labelNamer = new LabelNamer();

    @Test
    @DisplayName("요청한 이름을 사용할 수 있으면 그대로 배정한다")
    void assign_ReturnsRequestedName_WhenAvailable() {
        assertThat(labelNamer.assign("친구", Set.of("친구 (2)")).value()).isEqualTo("친구");
    }

    @Test
    @DisplayName("같은 이름이 있으면 2부터 시작해 가장 작은 빈 번호를 사용한다")
    void assign_UsesLowestAvailableSuffix_WhenNameIsOccupied() {
        assertThat(labelNamer.assign("친구", Set.of("친구", "친구 (3)")).value()).isEqualTo("친구 (2)");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" \t", "가나다라마바사아자차카타파하가나다라마바사"})
    @DisplayName("비어 있거나 20자를 초과한 원래 이름은 배정할 수 없다")
    void assign_RejectsInvalidName_BeforeAddingSuffix(String requestedName) {
        Set<String> occupiedNames = new HashSet<>();
        occupiedNames.add(requestedName);

        assertThatThrownBy(() -> labelNamer.assign(requestedName, occupiedNames))
                .isInstanceOf(InvalidLabelNameException.class);
    }

    @Test
    @DisplayName("20자 이름은 충돌할 때 접미사 길이에 맞춰 줄이고 완성된 후보의 중복을 검사한다")
    void assign_ShortensName_AfterCollision() {
        // given
        String requestedName = "가".repeat(20);
        String firstCandidate = "가".repeat(16) + " (2)";

        // when & then
        assertThat(labelNamer.assign(requestedName, Set.of()).value()).isEqualTo(requestedName);
        assertThat(labelNamer.assign(requestedName, Set.of(requestedName, firstCandidate)).value())
                .isEqualTo("가".repeat(16) + " (3)");
    }

    @Test
    @DisplayName("두 자리 번호를 붙이면 접미사가 늘어난 길이만큼 원래 이름을 더 줄인다")
    void assign_ShortensNameMore_WhenSuffixGrows() {
        // given
        String requestedName = "가".repeat(20);
        Set<String> occupiedNames = new HashSet<>();
        occupiedNames.add(requestedName);
        for (int number = 2; number <= 9; number++) {
            occupiedNames.add("가".repeat(16) + " (" + number + ")");
        }

        // when & then
        assertThat(labelNamer.assign(requestedName, occupiedNames).value())
                .isEqualTo("가".repeat(15) + " (10)");
    }

    @Test
    @DisplayName("이름을 줄일 때 경계에 걸린 서로게이트 쌍을 통째로 제외한다")
    void assign_PreservesSurrogatePairs_WhenShortening() {
        String requestedName = "가".repeat(15) + "😀나다라";

        assertThat(labelNamer.assign(requestedName, Set.of(requestedName)).value())
                .isEqualTo("가".repeat(15) + " (2)");
    }
}
