package com.example.DunbarHorizon.flag.application.service.memorial;

import com.example.DunbarHorizon.flag.domain.exception.FlagErrorCode;
import com.example.DunbarHorizon.flag.domain.flag.Flag;
import com.example.DunbarHorizon.flag.domain.flag.FlagSchedule;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagNotFoundException;
import com.example.DunbarHorizon.flag.domain.flag.repository.FlagRepository;
import com.example.DunbarHorizon.flag.domain.memorial.exception.FlagMemorialAuthorizationException;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagAuthorizationException;
import com.example.DunbarHorizon.flag.domain.memorial.FlagMemorial;
import com.example.DunbarHorizon.flag.domain.memorial.exception.FlagMemorialNotFoundException;
import com.example.DunbarHorizon.flag.domain.memorial.repository.FlagMemorialRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class FlagMemorialCommandServiceTest {

    @InjectMocks private FlagMemorialCommandService flagMemorialCommandService;

    @Mock private FlagRepository flagRepository;
    @Mock private FlagMemorialRepository memorialRepository;

    private static final Long FLAG_ID = 1L;
    private static final Long HOST_ID = 10L;
    private static final Long MEMORIAL_ID = 100L;
    private static final Long WRITER_ID = 2L;
    private static final Long OTHER_ID = 99L;

    private Flag endedFlag() {
        LocalDateTime now = LocalDateTime.now();
        Flag flag = Flag.create(HOST_ID, "모임", "설명", 10,
                FlagSchedule.of(now.minusHours(3), now.minusHours(2), now.minusHours(1)));
        ReflectionTestUtils.setField(flag, "id", FLAG_ID);
        return flag;
    }

    @Test
    @DisplayName("참여자의 후기 생성은 Flag를 읽고 참여 여부를 조회해 후기만 저장한다")
    void createMemorial_Success_Participant() {
        // given
        Flag flag = endedFlag();
        given(flagRepository.findById(FLAG_ID)).willReturn(Optional.of(flag));
        given(flagRepository.isParticipating(FLAG_ID, WRITER_ID)).willReturn(true);
        given(memorialRepository.save(any(FlagMemorial.class))).willAnswer(invocation -> {
            FlagMemorial saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", MEMORIAL_ID);
            return saved;
        });

        // when
        Long result = flagMemorialCommandService.createMemorial(FLAG_ID, WRITER_ID, "후기");

        // then
        assertThat(result).isEqualTo(MEMORIAL_ID);
        ArgumentCaptor<FlagMemorial> captor = ArgumentCaptor.forClass(FlagMemorial.class);
        InOrder order = inOrder(flagRepository, memorialRepository);
        order.verify(flagRepository).findById(FLAG_ID);
        order.verify(flagRepository).isParticipating(FLAG_ID, WRITER_ID);
        order.verify(memorialRepository).save(captor.capture());
        verify(flagRepository, never()).findByIdForUpdate(any());
        verify(flagRepository, never()).save(any());
        assertThat(flag.isAutoExpiryExempt()).isFalse();
        assertThat(captor.getValue().getFlagId()).isEqualTo(FLAG_ID);
        assertThat(captor.getValue().getWriterId()).isEqualTo(WRITER_ID);
        assertThat(captor.getValue().getContent()).isEqualTo("후기");
    }

    @Test
    @DisplayName("호스트는 참여 여부를 조회하지 않고 후기를 생성한다")
    void createMemorial_Success_Host() {
        // given
        Flag flag = endedFlag();
        given(flagRepository.findById(FLAG_ID)).willReturn(Optional.of(flag));
        given(memorialRepository.save(any(FlagMemorial.class))).willAnswer(invocation -> {
            FlagMemorial saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", MEMORIAL_ID);
            return saved;
        });

        // when
        Long result = flagMemorialCommandService.createMemorial(FLAG_ID, HOST_ID, "후기");

        // then
        assertThat(result).isEqualTo(MEMORIAL_ID);
        verify(flagRepository, never()).isParticipating(anyLong(), anyLong());
    }

    @Test
    @DisplayName("참여하지 않은 사용자의 후기는 저장하지 않는다")
    void createMemorial_Fail_Stranger() {
        // given
        given(flagRepository.findById(FLAG_ID)).willReturn(Optional.of(endedFlag()));
        given(flagRepository.isParticipating(FLAG_ID, OTHER_ID)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> flagMemorialCommandService.createMemorial(FLAG_ID, OTHER_ID, "후기"))
                .isInstanceOf(FlagMemorialAuthorizationException.class);
        verifyNoInteractions(memorialRepository);
        verify(flagRepository, never()).save(any());
    }

    @Test
    @DisplayName("Flag가 없으면 후기를 생성하지 않는다")
    void createMemorial_Fail_FlagNotFound() {
        // given
        given(flagRepository.findById(FLAG_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> flagMemorialCommandService.createMemorial(FLAG_ID, WRITER_ID, "후기"))
                .isInstanceOf(FlagNotFoundException.class);
        verifyNoInteractions(memorialRepository);
    }

    @Test
    @DisplayName("후기 삭제는 후기만 지우고 Flag는 잠그거나 저장하지 않는다")
    void deleteMemorial_Success_Writer() {
        // given
        Flag flag = endedFlag();
        FlagMemorial memorial = FlagMemorial.create(flag, WRITER_ID, "후기", true);
        given(memorialRepository.findById(MEMORIAL_ID)).willReturn(Optional.of(memorial));

        // when
        flagMemorialCommandService.deleteMemorial(FLAG_ID, MEMORIAL_ID, WRITER_ID);

        // then
        verify(memorialRepository).delete(memorial);
        verifyNoInteractions(flagRepository);
    }

    @Test
    @DisplayName("존재하지 않는 후기는 삭제할 수 없다")
    void deleteMemorial_Fail_MemorialNotFound() {
        // given
        given(memorialRepository.findById(MEMORIAL_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> flagMemorialCommandService.deleteMemorial(FLAG_ID, MEMORIAL_ID, WRITER_ID))
                .isInstanceOf(FlagMemorialNotFoundException.class);
        verify(memorialRepository, never()).delete(any());
    }

    @Test
    @DisplayName("다른 Flag의 후기는 삭제할 수 없다")
    void deleteMemorial_Fail_BelongsToOtherFlag() {
        // given
        Flag flag = endedFlag();
        FlagMemorial memorial = FlagMemorial.create(flag, WRITER_ID, "후기", true);
        given(memorialRepository.findById(MEMORIAL_ID)).willReturn(Optional.of(memorial));

        // when & then
        assertThatThrownBy(() -> flagMemorialCommandService.deleteMemorial(OTHER_ID, MEMORIAL_ID, WRITER_ID))
                .isInstanceOf(FlagMemorialNotFoundException.class);
        verify(memorialRepository, never()).delete(any());
    }

    @Test
    @DisplayName("작성자가 아닌 사용자의 삭제 요청은 후기를 지우지 않는다")
    void deleteMemorial_Fail_NonWriter() {
        // given
        Flag flag = endedFlag();
        FlagMemorial memorial = FlagMemorial.create(flag, WRITER_ID, "후기", true);
        given(memorialRepository.findById(MEMORIAL_ID)).willReturn(Optional.of(memorial));

        // when & then
        assertThatThrownBy(() -> flagMemorialCommandService.deleteMemorial(FLAG_ID, MEMORIAL_ID, OTHER_ID))
                .isInstanceOf(FlagAuthorizationException.class);
        verify(memorialRepository, never()).delete(any());
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
