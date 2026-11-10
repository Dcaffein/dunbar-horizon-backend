package com.example.DunbarHorizon.flag.domain.flag;

import com.example.DunbarHorizon.flag.domain.flag.event.FlagConclusionKind;
import com.example.DunbarHorizon.flag.domain.flag.repository.FlagRepository;
import com.example.DunbarHorizon.flag.domain.memorial.repository.FlagMemorialRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class FlagConclusionDeciderTest {

    private static final Long FLAG_ID = 1L;
    private static final Long HOST_ID = 10L;

    @InjectMocks private FlagConclusionDecider decider;
    @Mock private FlagRepository flagRepository;
    @Mock private FlagMemorialRepository memorialRepository;

    private Flag endedFlag() {
        LocalDateTime now = LocalDateTime.now();
        Flag flag = Flag.create(HOST_ID, "종료된 모임", "설명", 10,
                FlagSchedule.of(now.minusHours(30), now.minusHours(29), now.minusHours(26)));
        ReflectionTestUtils.setField(flag, "id", FLAG_ID);
        return flag;
    }

    @Test
    @DisplayName("후기가 있으면 보존한다")
    void conclude_WithMemorial_Preserves() {
        // given
        Flag flag = endedFlag();
        given(memorialRepository.existsByFlagId(FLAG_ID)).willReturn(true);

        // when
        FlagConclusionKind kind = decider.conclude(flag, LocalDateTime.now());

        // then
        assertThat(kind).isEqualTo(FlagConclusionKind.PRESERVED);
        assertThat(flag.isAutoExpiryExempt()).isTrue();
        assertThat(flag.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("후기가 없어도 앙코르가 있으면 보존한다")
    void conclude_WithEncoreOnly_Preserves() {
        // given
        Flag flag = endedFlag();
        given(memorialRepository.existsByFlagId(FLAG_ID)).willReturn(false);
        given(flagRepository.existsByParentId(FLAG_ID)).willReturn(true);

        // when
        FlagConclusionKind kind = decider.conclude(flag, LocalDateTime.now());

        // then
        assertThat(kind).isEqualTo(FlagConclusionKind.PRESERVED);
        assertThat(flag.isAutoExpiryExempt()).isTrue();
    }

    @Test
    @DisplayName("후기도 앙코르도 없으면 주어진 시각으로 만료한다")
    void conclude_WithoutTrace_Expires() {
        // given
        Flag flag = endedFlag();
        LocalDateTime now = LocalDateTime.now();
        given(memorialRepository.existsByFlagId(FLAG_ID)).willReturn(false);
        given(flagRepository.existsByParentId(FLAG_ID)).willReturn(false);

        // when
        FlagConclusionKind kind = decider.conclude(flag, now);

        // then
        assertThat(kind).isEqualTo(FlagConclusionKind.ENDED);
        assertThat(flag.getDeletedAt()).isEqualTo(now);
        assertThat(flag.isAutoExpiryExempt()).isFalse();
    }
}
