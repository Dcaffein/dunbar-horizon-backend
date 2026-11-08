package com.example.DunbarHorizon.flag.domain.memorial;

import com.example.DunbarHorizon.flag.domain.exception.FlagErrorCode;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagAuthorizationException;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagNotFoundException;
import com.example.DunbarHorizon.flag.domain.flag.repository.FlagRepository;
import com.example.DunbarHorizon.flag.domain.flag.Flag;
import com.example.DunbarHorizon.flag.domain.flag.FlagSchedule;
import com.example.DunbarHorizon.flag.domain.flag.FlagExpiryExemptionUpdater;
import com.example.DunbarHorizon.flag.domain.memorial.exception.FlagMemorialNotFoundException;
import com.example.DunbarHorizon.flag.domain.memorial.repository.FlagMemorialRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class FlagMemorialManagerTest {

    @InjectMocks private FlagMemorialManager memorialManager;

    @Mock private FlagRepository flagRepository;
    @Mock private FlagMemorialRepository memorialRepository;
    @Mock private FlagMemorialFactory memorialFactory;
    @Mock private FlagExpiryExemptionUpdater exemptionUpdater;

    private static final Long FLAG_ID = 1L;
    private static final Long MEMORIAL_ID = 10L;
    private static final Long WRITER_ID = 2L;
    private static final Long OTHER_ID = 99L;
    private static final LocalDateTime NOW = LocalDateTime.now();

    private Flag createEndedFlag() {
        Flag flag = Flag.create(1L, "종료된 플래그", "설명", 10,
                FlagSchedule.of(NOW.minusHours(3), NOW.minusHours(2), NOW.minusHours(1)));
        ReflectionTestUtils.setField(flag, "id", FLAG_ID);
        return flag;
    }

    @Test
    @DisplayName("Memorial 작성은 Flag를 잠그고 Memorial을 저장한 뒤 면제를 다시 계산해 저장한다")
    void writeMemorial_LocksSavesThenRefreshesExemption() {
        // given
        Flag flag = createEndedFlag();
        FlagMemorial memorial = mock(FlagMemorial.class);
        given(flagRepository.findByIdForUpdate(FLAG_ID)).willReturn(Optional.of(flag));
        given(memorialFactory.create(flag, WRITER_ID, "내용")).willReturn(memorial);
        given(memorialRepository.save(memorial)).willReturn(memorial);

        // when
        FlagMemorial result = memorialManager.writeMemorial(FLAG_ID, WRITER_ID, "내용");

        // then
        assertThat(result).isSameAs(memorial);
        InOrder inOrder = inOrder(flagRepository, memorialFactory, memorialRepository, exemptionUpdater);
        inOrder.verify(flagRepository).findByIdForUpdate(FLAG_ID);
        inOrder.verify(memorialFactory).create(flag, WRITER_ID, "내용");
        inOrder.verify(memorialRepository).save(memorial);
        inOrder.verify(exemptionUpdater).refresh(flag);
        inOrder.verify(flagRepository).save(flag);
    }

    @Test
    @DisplayName("존재하지 않는 Flag에 Memorial을 작성하면 FlagNotFoundException이 발생한다")
    void writeMemorial_FlagNotFound_ThrowsException() {
        // given
        given(flagRepository.findByIdForUpdate(FLAG_ID)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> memorialManager.writeMemorial(FLAG_ID, WRITER_ID, "내용"))
                .isInstanceOf(FlagNotFoundException.class);
        verifyNoInteractions(memorialFactory, memorialRepository, exemptionUpdater);
    }

    @Test
    @DisplayName("Memorial 삭제는 Memorial을 읽기 전에 Flag를 잠그고, 삭제 뒤 면제를 다시 계산해 저장한다")
    void deleteMemorial_LocksBeforeReadingThenRefreshesExemption() {
        // given
        Flag flag = createEndedFlag();
        FlagMemorial memorial = mock(FlagMemorial.class);
        given(memorial.getFlagId()).willReturn(FLAG_ID);
        given(flagRepository.findByIdForUpdate(FLAG_ID)).willReturn(Optional.of(flag));
        given(memorialRepository.findById(MEMORIAL_ID)).willReturn(Optional.of(memorial));

        // when
        memorialManager.deleteMemorial(FLAG_ID, MEMORIAL_ID, WRITER_ID);

        // then
        InOrder inOrder = inOrder(flagRepository, memorialRepository, memorial, exemptionUpdater);
        inOrder.verify(flagRepository).findByIdForUpdate(FLAG_ID);
        inOrder.verify(memorialRepository).findById(MEMORIAL_ID);
        inOrder.verify(memorial).validateDeletion(WRITER_ID);
        inOrder.verify(memorialRepository).delete(memorial);
        inOrder.verify(exemptionUpdater).refresh(flag);
        inOrder.verify(flagRepository).save(flag);
    }

    @Test
    @DisplayName("Flag가 없으면 Memorial을 읽지 않고 FlagNotFoundException으로 삭제를 거절한다")
    void deleteMemorial_FlagNotFound_ThrowsBeforeReadingMemorial() {
        // given
        given(flagRepository.findByIdForUpdate(FLAG_ID)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> memorialManager.deleteMemorial(FLAG_ID, MEMORIAL_ID, WRITER_ID))
                .isInstanceOf(FlagNotFoundException.class);
        verifyNoInteractions(memorialRepository, exemptionUpdater);
    }

    @Test
    @DisplayName("존재하지 않는 Memorial을 삭제하면 FlagMemorialNotFoundException이 발생한다")
    void deleteMemorial_MemorialNotFound_ThrowsException() {
        // given
        given(flagRepository.findByIdForUpdate(FLAG_ID)).willReturn(Optional.of(createEndedFlag()));
        given(memorialRepository.findById(MEMORIAL_ID)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> memorialManager.deleteMemorial(FLAG_ID, MEMORIAL_ID, WRITER_ID))
                .isInstanceOf(FlagMemorialNotFoundException.class);
        verify(memorialRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Memorial이 다른 Flag에 속하면 FlagMemorialNotFoundException이 발생한다")
    void deleteMemorial_BelongsToOtherFlag_ThrowsException() {
        // given
        FlagMemorial memorial = mock(FlagMemorial.class);
        given(memorial.getFlagId()).willReturn(OTHER_ID);
        given(flagRepository.findByIdForUpdate(FLAG_ID)).willReturn(Optional.of(createEndedFlag()));
        given(memorialRepository.findById(MEMORIAL_ID)).willReturn(Optional.of(memorial));

        // when / then
        assertThatThrownBy(() -> memorialManager.deleteMemorial(FLAG_ID, MEMORIAL_ID, WRITER_ID))
                .isInstanceOf(FlagMemorialNotFoundException.class);
        verify(memorialRepository, never()).delete(any());
    }

    @Test
    @DisplayName("작성자가 아닌 사용자가 Memorial을 삭제하면 FlagAuthorizationException이 발생한다")
    void deleteMemorial_ByNonWriter_ThrowsException() {
        // given
        FlagMemorial memorial = mock(FlagMemorial.class);
        given(memorial.getFlagId()).willReturn(FLAG_ID);
        given(flagRepository.findByIdForUpdate(FLAG_ID)).willReturn(Optional.of(createEndedFlag()));
        given(memorialRepository.findById(MEMORIAL_ID)).willReturn(Optional.of(memorial));
        willThrow(new FlagAuthorizationException(FlagErrorCode.FLAG_MEMORIAL_AUTHOR_ONLY))
                .given(memorial).validateDeletion(OTHER_ID);

        // when / then
        assertThatThrownBy(() -> memorialManager.deleteMemorial(FLAG_ID, MEMORIAL_ID, OTHER_ID))
                .isInstanceOf(FlagAuthorizationException.class);
        verify(memorialRepository, never()).delete(any());
    }
}
