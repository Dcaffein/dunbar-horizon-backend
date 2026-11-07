package com.example.DunbarHorizon.flag.application.eventListener;

import com.example.DunbarHorizon.flag.domain.flag.Flag;
import com.example.DunbarHorizon.flag.domain.flag.FlagExpiryExemptionUpdater;
import com.example.DunbarHorizon.flag.domain.flag.FlagStatus;
import com.example.DunbarHorizon.flag.domain.flag.event.FlagDeletedEvent;
import com.example.DunbarHorizon.flag.domain.flag.repository.FlagRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class FlagEncoreEventListenerTest {

    @InjectMocks private FlagEncoreEventListener listener;

    @Mock private FlagExpiryExemptionUpdater flagExpiryExemptionUpdater;
    @Mock private FlagRepository flagRepository;

    private static final Long FLAG_ID = 1L;
    private static final Long PARENT_ID = 99L;
    private static final Long HOST_ID = 10L;

    private FlagDeletedEvent deletedEvent(Long parentId) {
        return new FlagDeletedEvent(FLAG_ID, HOST_ID, parentId, "앙코르", FlagStatus.RECRUITING);
    }

    @Test
    @DisplayName("앙코르가 삭제되면 부모의 만료 면제를 다시 계산해 저장한다")
    void handleEncoreDeleted_RefreshesParent() {
        // given
        Flag parent = mock(Flag.class);
        given(parent.getId()).willReturn(PARENT_ID);
        given(flagRepository.findById(PARENT_ID)).willReturn(Optional.of(parent));
        given(flagExpiryExemptionUpdater.refresh(PARENT_ID)).willReturn(parent);

        // when
        listener.handleEncoreDeleted(deletedEvent(PARENT_ID));

        // then
        verify(flagRepository).save(parent);
    }

    @Test
    @DisplayName("앙코르가 아닌 Flag가 삭제되면 아무것도 갱신하지 않는다")
    void handleEncoreDeleted_Ignores_WhenNoParent() {
        // when
        listener.handleEncoreDeleted(deletedEvent(null));

        // then
        verifyNoInteractions(flagRepository, flagExpiryExemptionUpdater);
    }

    @Test
    @DisplayName("부모가 이미 삭제됐으면 예외 없이 갱신을 건너뛴다")
    void handleEncoreDeleted_Skips_WhenParentAlreadyDeleted() {
        // given
        given(flagRepository.findById(PARENT_ID)).willReturn(Optional.empty());

        // when
        listener.handleEncoreDeleted(deletedEvent(PARENT_ID));

        // then
        verifyNoInteractions(flagExpiryExemptionUpdater);
    }
}
