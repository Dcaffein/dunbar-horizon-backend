package com.example.DunbarHorizon.flag.application.service.memorial;

import com.example.DunbarHorizon.flag.domain.exception.FlagErrorCode;
import com.example.DunbarHorizon.flag.domain.memorial.FlagMemorialManager;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagAuthorizationException;
import com.example.DunbarHorizon.flag.domain.memorial.FlagMemorial;
import com.example.DunbarHorizon.flag.domain.memorial.exception.FlagMemorialNotFoundException;
import com.example.DunbarHorizon.flag.domain.memorial.repository.FlagMemorialRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FlagMemorialCommandServiceTest {

    @InjectMocks private FlagMemorialCommandService flagMemorialCommandService;

    @Mock private FlagMemorialManager memorialManager;
    @Mock private FlagMemorialRepository memorialRepository;

    private static final Long FLAG_ID = 1L;
    private static final Long WRITER_ID = 2L;
    private static final Long OTHER_ID = 99L;

    @Test
    @DisplayName("Memorial 작성은 FlagMemorialManager에 위임하고 생성된 ID를 반환한다")
    void createMemorial_DelegatesAndReturnsId() {
        // given
        FlagMemorial memorial = mock(FlagMemorial.class);
        given(memorial.getId()).willReturn(1L);
        given(memorialManager.writeMemorial(FLAG_ID, WRITER_ID, "추도 내용")).willReturn(memorial);

        // when
        Long result = flagMemorialCommandService.createMemorial(FLAG_ID, WRITER_ID, "추도 내용");

        // then
        assertThat(result).isEqualTo(1L);
    }

    @Test
    @DisplayName("Memorial 삭제는 FlagMemorialManager에 위임한다")
    void deleteMemorial_Delegates() {
        // when
        flagMemorialCommandService.deleteMemorial(FLAG_ID, 1L, WRITER_ID);

        // then
        verify(memorialManager).deleteMemorial(FLAG_ID, 1L, WRITER_ID);
    }

    @Test
    @DisplayName("작성자가 Memorial을 수정할 수 있다")
    void updateMemorial_ByWriter_Success() {
        // given
        FlagMemorial mockMemorial = mock(FlagMemorial.class);
        given(mockMemorial.getFlagId()).willReturn(FLAG_ID);
        given(memorialRepository.findById(1L)).willReturn(Optional.of(mockMemorial));

        // when
        flagMemorialCommandService.updateMemorial(FLAG_ID, 1L, WRITER_ID, "수정된 내용");

        // then
        verify(mockMemorial).updateContent(WRITER_ID, "수정된 내용");
    }

    @Test
    @DisplayName("작성자가 아닌 사용자가 Memorial을 수정하면 FlagAuthorizationException이 발생한다")
    void updateMemorial_ByNonWriter_ThrowsException() {
        // given
        FlagMemorial mockMemorial = mock(FlagMemorial.class);
        given(mockMemorial.getFlagId()).willReturn(FLAG_ID);
        given(memorialRepository.findById(1L)).willReturn(Optional.of(mockMemorial));
        willThrow(new FlagAuthorizationException(FlagErrorCode.FLAG_MEMORIAL_AUTHOR_ONLY))
                .given(mockMemorial).updateContent(OTHER_ID, "수정 시도");

        // when / then
        assertThatThrownBy(() -> flagMemorialCommandService.updateMemorial(FLAG_ID, 1L, OTHER_ID, "수정 시도"))
                .isInstanceOf(FlagAuthorizationException.class);
    }

    @Test
    @DisplayName("존재하지 않는 Memorial을 수정하면 FlagMemorialNotFoundException이 발생한다")
    void updateMemorial_NotFound_ThrowsException() {
        // given
        given(memorialRepository.findById(999L)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> flagMemorialCommandService.updateMemorial(FLAG_ID, 999L, WRITER_ID, "내용"))
                .isInstanceOf(FlagMemorialNotFoundException.class);
    }

    @Test
    @DisplayName("Memorial이 다른 Flag에 속하면 수정 시 FlagMemorialNotFoundException이 발생한다")
    void updateMemorial_BelongsToOtherFlag_ThrowsException() {
        // given
        FlagMemorial mockMemorial = mock(FlagMemorial.class);
        given(mockMemorial.getFlagId()).willReturn(FLAG_ID);
        given(memorialRepository.findById(1L)).willReturn(Optional.of(mockMemorial));

        // when / then
        assertThatThrownBy(() -> flagMemorialCommandService.updateMemorial(999L, 1L, WRITER_ID, "수정"))
                .isInstanceOf(FlagMemorialNotFoundException.class);
    }
}
