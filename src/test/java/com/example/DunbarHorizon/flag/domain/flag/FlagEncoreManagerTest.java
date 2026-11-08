package com.example.DunbarHorizon.flag.domain.flag;

import com.example.DunbarHorizon.flag.domain.flag.exception.FlagNotFoundException;
import com.example.DunbarHorizon.flag.domain.flag.repository.FlagRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class FlagEncoreManagerTest {

    @InjectMocks private FlagEncoreManager encoreManager;

    @Mock private FlagRepository flagRepository;
    @Mock private FlagEncoreFactory encoreFactory;
    @Mock private FlagExpiryExemptionUpdater exemptionUpdater;

    private static final Long PARENT_ID = 1L;
    private static final Long HOST_ID = 10L;
    private static final LocalDateTime NOW = LocalDateTime.now();
    private static final LocalDateTime DEADLINE = NOW.plusHours(1);
    private static final LocalDateTime START = NOW.plusHours(2);
    private static final LocalDateTime END = NOW.plusHours(3);

    private Flag createEndedFlag(Long id) {
        Flag flag = Flag.create(HOST_ID, "종료된 플래그", "설명", 10,
                FlagSchedule.of(NOW.minusHours(3), NOW.minusHours(2), NOW.minusHours(1)));
        ReflectionTestUtils.setField(flag, "id", id);
        return flag;
    }

    @Test
    @DisplayName("앵코르 생성은 부모를 잠그고 앵코르를 저장한 뒤 부모 면제를 다시 계산해 저장한다")
    void encore_LocksParentSavesThenRefreshesParentExemption() {
        // given
        Flag parent = createEndedFlag(PARENT_ID);
        Flag encore = createEndedFlag(2L);
        given(flagRepository.findByIdForUpdate(PARENT_ID)).willReturn(Optional.of(parent));
        given(encoreFactory.encore(parent, HOST_ID, DEADLINE, START, END)).willReturn(encore);
        given(flagRepository.save(encore)).willReturn(encore);

        // when
        Flag result = encoreManager.encore(PARENT_ID, HOST_ID, DEADLINE, START, END);

        // then
        assertThat(result).isSameAs(encore);
        InOrder inOrder = inOrder(flagRepository, encoreFactory, exemptionUpdater);
        inOrder.verify(flagRepository).findByIdForUpdate(PARENT_ID);
        inOrder.verify(encoreFactory).encore(parent, HOST_ID, DEADLINE, START, END);
        inOrder.verify(flagRepository).save(encore);
        inOrder.verify(exemptionUpdater).refresh(parent);
        inOrder.verify(flagRepository).save(parent);
    }

    @Test
    @DisplayName("존재하지 않는 부모로 앵코르를 만들면 FlagNotFoundException이 발생한다")
    void encore_ParentNotFound_ThrowsException() {
        // given
        given(flagRepository.findByIdForUpdate(PARENT_ID)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> encoreManager.encore(PARENT_ID, HOST_ID, DEADLINE, START, END))
                .isInstanceOf(FlagNotFoundException.class);
        verifyNoInteractions(encoreFactory, exemptionUpdater);
    }
}
