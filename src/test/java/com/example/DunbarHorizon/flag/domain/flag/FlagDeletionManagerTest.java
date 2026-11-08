package com.example.DunbarHorizon.flag.domain.flag;

import com.example.DunbarHorizon.flag.domain.flag.exception.FlagAuthorizationException;
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
class FlagDeletionManagerTest {

    @InjectMocks private FlagDeletionManager deletionManager;

    @Mock private FlagRepository flagRepository;
    @Mock private FlagExpiryExemptionUpdater exemptionUpdater;

    private static final Long FLAG_ID = 2L;
    private static final Long PARENT_ID = 1L;
    private static final Long HOST_ID = 10L;
    private static final Long OTHER_ID = 99L;
    private static final LocalDateTime NOW = LocalDateTime.now();

    private Flag createEndedFlag(Long id, Long parentId) {
        Flag flag = Flag.create(HOST_ID, "종료된 플래그", "설명", 10,
                FlagSchedule.of(NOW.minusHours(3), NOW.minusHours(2), NOW.minusHours(1)));
        ReflectionTestUtils.setField(flag, "id", id);
        ReflectionTestUtils.setField(flag, "parentId", parentId);
        return flag;
    }

    @Test
    @DisplayName("부모가 없는 플래그를 삭제하면 그 플래그만 잠그고 지운다")
    void delete_FlagWithoutParent_LocksAndDeletesOnlyIt() {
        // given
        Flag flag = createEndedFlag(FLAG_ID, null);
        given(flagRepository.findByIdForUpdate(FLAG_ID)).willReturn(Optional.of(flag));

        // when
        deletionManager.delete(FLAG_ID, HOST_ID);

        // then
        assertThat(flag.isDeleted()).isTrue();
        verify(flagRepository).save(flag);
        verifyNoInteractions(exemptionUpdater);
    }

    @Test
    @DisplayName("앵코르를 삭제하면 앵코르를 먼저 잠그고 지운 뒤 부모를 잠가 면제를 다시 계산한다")
    void delete_Encore_LocksEncoreThenParentAndRefreshesParent() {
        // given
        Flag encore = createEndedFlag(FLAG_ID, PARENT_ID);
        Flag parent = createEndedFlag(PARENT_ID, null);
        given(flagRepository.findByIdForUpdate(FLAG_ID)).willReturn(Optional.of(encore));
        given(flagRepository.findByIdForUpdate(PARENT_ID)).willReturn(Optional.of(parent));

        // when
        deletionManager.delete(FLAG_ID, HOST_ID);

        // then
        assertThat(encore.isDeleted()).isTrue();
        InOrder inOrder = inOrder(flagRepository, exemptionUpdater);
        inOrder.verify(flagRepository).findByIdForUpdate(FLAG_ID);
        inOrder.verify(flagRepository).save(encore);
        inOrder.verify(flagRepository).findByIdForUpdate(PARENT_ID);
        inOrder.verify(exemptionUpdater).refresh(parent);
        inOrder.verify(flagRepository).save(parent);
    }

    @Test
    @DisplayName("부모가 이미 삭제된 앵코르는 부모 갱신 없이 지운다")
    void delete_EncoreWithDeletedParent_SkipsParentRefresh() {
        // given
        Flag encore = createEndedFlag(FLAG_ID, PARENT_ID);
        given(flagRepository.findByIdForUpdate(FLAG_ID)).willReturn(Optional.of(encore));
        given(flagRepository.findByIdForUpdate(PARENT_ID)).willReturn(Optional.empty());

        // when
        deletionManager.delete(FLAG_ID, HOST_ID);

        // then
        assertThat(encore.isDeleted()).isTrue();
        verifyNoInteractions(exemptionUpdater);
    }

    @Test
    @DisplayName("존재하지 않는 플래그를 삭제하면 FlagNotFoundException이 발생한다")
    void delete_FlagNotFound_ThrowsException() {
        // given
        given(flagRepository.findByIdForUpdate(FLAG_ID)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> deletionManager.delete(FLAG_ID, HOST_ID))
                .isInstanceOf(FlagNotFoundException.class);
        verifyNoInteractions(exemptionUpdater);
    }

    @Test
    @DisplayName("호스트가 아닌 사용자가 삭제하면 FlagAuthorizationException이 발생하고 저장하지 않는다")
    void delete_ByNonHost_ThrowsException() {
        // given
        Flag flag = createEndedFlag(FLAG_ID, PARENT_ID);
        given(flagRepository.findByIdForUpdate(FLAG_ID)).willReturn(Optional.of(flag));

        // when / then
        assertThatThrownBy(() -> deletionManager.delete(FLAG_ID, OTHER_ID))
                .isInstanceOf(FlagAuthorizationException.class);
        verify(flagRepository, never()).save(any());
        verifyNoInteractions(exemptionUpdater);
    }
}
