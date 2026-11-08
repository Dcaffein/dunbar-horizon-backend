package com.example.DunbarHorizon.flag.domain.flag;

import com.example.DunbarHorizon.flag.domain.flag.exception.FlagAuthorizationException;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagInvalidStatusException;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class FlagEncoreManagerTest {

    @InjectMocks private FlagEncoreManager encoreManager;

    @Mock private FlagRepository flagRepository;
    @Mock private FlagExpiryExemptionUpdater exemptionUpdater;

    private static final Long PARENT_ID = 1L;
    private static final Long HOST_ID = 10L;
    private static final Long OTHER_ID = 99L;
    private static final LocalDateTime NOW = LocalDateTime.now();
    private static final LocalDateTime DEADLINE = NOW.plusHours(1);
    private static final LocalDateTime START = NOW.plusHours(2);
    private static final LocalDateTime END = NOW.plusHours(3);

    private Flag flagWith(FlagSchedule schedule) {
        Flag flag = Flag.create(HOST_ID, "원본 플래그", "설명", 10, schedule);
        ReflectionTestUtils.setField(flag, "id", PARENT_ID);
        return flag;
    }

    private Flag endedParent() {
        return flagWith(FlagSchedule.of(NOW.minusHours(3), NOW.minusHours(2), NOW.minusHours(1)));
    }

    @Test
    @DisplayName("앵코르 생성은 부모를 잠그고 앵코르를 저장한 뒤 부모 면제를 다시 계산해 저장한다")
    void encore_LocksParentSavesThenRefreshesParentExemption() {
        // given
        Flag parent = endedParent();
        given(flagRepository.findByIdForUpdate(PARENT_ID)).willReturn(Optional.of(parent));
        given(flagRepository.existsByParentId(PARENT_ID)).willReturn(false);
        given(flagRepository.save(any(Flag.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        Flag encore = encoreManager.encore(PARENT_ID, HOST_ID, DEADLINE, START, END);

        // then
        assertThat(encore.getParentId()).isEqualTo(PARENT_ID);
        InOrder inOrder = inOrder(flagRepository, exemptionUpdater);
        inOrder.verify(flagRepository).findByIdForUpdate(PARENT_ID);
        inOrder.verify(flagRepository).existsByParentId(PARENT_ID);
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
        verifyNoInteractions(exemptionUpdater);
    }

    @Test
    @DisplayName("호스트가 아닌 사용자가 앵코르를 만들면 FlagAuthorizationException이 발생한다")
    void encore_ByNonHost_ThrowsException() {
        // given
        given(flagRepository.findByIdForUpdate(PARENT_ID)).willReturn(Optional.of(endedParent()));

        // when / then
        assertThatThrownBy(() -> encoreManager.encore(PARENT_ID, OTHER_ID, DEADLINE, START, END))
                .isInstanceOf(FlagAuthorizationException.class);
        verify(flagRepository, never()).save(any());
    }

    @Test
    @DisplayName("이미 앵코르가 있는 부모에 앵코르를 만들면 FlagInvalidStatusException이 발생한다")
    void encore_AlreadyExists_ThrowsException() {
        // given
        given(flagRepository.findByIdForUpdate(PARENT_ID)).willReturn(Optional.of(endedParent()));
        given(flagRepository.existsByParentId(PARENT_ID)).willReturn(true);

        // when / then
        assertThatThrownBy(() -> encoreManager.encore(PARENT_ID, HOST_ID, DEADLINE, START, END))
                .isInstanceOf(FlagInvalidStatusException.class);
        verify(flagRepository, never()).save(any());
    }

    @Test
    @DisplayName("종료되지 않은 부모에는 앵코르를 만들 수 없다")
    void encore_ParentNotEnded_ThrowsException() {
        // given
        Flag recruiting = flagWith(FlagSchedule.of(NOW.plusHours(1), NOW.plusHours(2), NOW.plusHours(3)));
        given(flagRepository.findByIdForUpdate(PARENT_ID)).willReturn(Optional.of(recruiting));
        given(flagRepository.existsByParentId(PARENT_ID)).willReturn(false);

        // when / then
        assertThatThrownBy(() -> encoreManager.encore(PARENT_ID, HOST_ID,
                NOW.plusHours(4), NOW.plusHours(5), NOW.plusHours(6)))
                .isInstanceOf(FlagInvalidStatusException.class);
        verify(flagRepository, never()).save(any());
    }
}
