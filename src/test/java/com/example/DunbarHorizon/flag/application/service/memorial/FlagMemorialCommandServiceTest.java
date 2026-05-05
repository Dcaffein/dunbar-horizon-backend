package com.example.DunbarHorizon.flag.application.service.memorial;

import com.example.DunbarHorizon.flag.domain.flag.Flag;
import com.example.DunbarHorizon.flag.domain.flag.FlagSchedule;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagAuthorizationException;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagNotFoundException;
import com.example.DunbarHorizon.flag.domain.flag.repository.FlagRepository;
import com.example.DunbarHorizon.flag.domain.memorial.DeletableFlagMemorial;
import com.example.DunbarHorizon.flag.domain.memorial.FlagMemorial;
import com.example.DunbarHorizon.flag.domain.memorial.FlagMemorialCreator;
import com.example.DunbarHorizon.flag.domain.memorial.event.MemorialCreatedEvent;
import com.example.DunbarHorizon.flag.domain.memorial.event.MemorialDeletedEvent;
import com.example.DunbarHorizon.flag.domain.memorial.exception.FlagMemorialNotFoundException;
import com.example.DunbarHorizon.flag.domain.memorial.repository.FlagMemorialRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FlagMemorialCommandServiceTest {

    @InjectMocks private FlagMemorialCommandService flagMemorialCommandService;

    @Mock private FlagRepository flagRepository;
    @Mock private FlagMemorialRepository memorialRepository;
    @Mock private FlagMemorialCreator memorialCreator;
    @Mock private ApplicationEventPublisher eventPublisher;

    private static final Long FLAG_ID = 1L;
    private static final Long WRITER_ID = 2L;
    private static final Long OTHER_ID = 99L;
    private static final LocalDateTime NOW = LocalDateTime.now();

    private Flag createEndedFlag() {
        FlagSchedule schedule = FlagSchedule.of(NOW.minusHours(3), NOW.minusHours(2), NOW.minusHours(1));
        Flag flag = Flag.create(1L, "종료된 플래그", "설명", 10, schedule);
        ReflectionTestUtils.setField(flag, "id", FLAG_ID);
        return flag;
    }

    @Test
    @DisplayName("정상적으로 추도문을 생성하고 ID를 반환한다")
    void createMemorial_Success() {
        // given
        Flag flag = createEndedFlag();
        FlagMemorial mockMemorial = mock(FlagMemorial.class);
        given(mockMemorial.getId()).willReturn(1L);

        given(flagRepository.findById(FLAG_ID)).willReturn(Optional.of(flag));
        given(memorialCreator.create(any(Flag.class), eq(WRITER_ID), eq("추도 내용"))).willReturn(mockMemorial);
        given(memorialRepository.save(any(FlagMemorial.class))).willReturn(mockMemorial);

        // when
        Long result = flagMemorialCommandService.createMemorial(FLAG_ID, WRITER_ID, "추도 내용");

        // then
        assertThat(result).isEqualTo(1L);
        verify(eventPublisher).publishEvent(any(MemorialCreatedEvent.class));
    }

    @Test
    @DisplayName("존재하지 않는 플래그에 추도문을 작성하면 FlagNotFoundException이 발생한다")
    void createMemorial_FlagNotFound_ThrowsException() {
        // given
        given(flagRepository.findById(999L)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> flagMemorialCommandService.createMemorial(999L, WRITER_ID, "내용"))
                .isInstanceOf(FlagNotFoundException.class);
    }

    @Test
    @DisplayName("작성자가 추도문을 수정할 수 있다")
    void updateMemorial_ByWriter_Success() {
        // given
        FlagMemorial mockMemorial = mock(FlagMemorial.class);
        given(memorialRepository.findById(1L)).willReturn(Optional.of(mockMemorial));

        // when
        flagMemorialCommandService.updateMemorial(1L, WRITER_ID, "수정된 내용");

        // then
        verify(mockMemorial).updateContent(WRITER_ID, "수정된 내용");
    }

    @Test
    @DisplayName("작성자가 아닌 사용자가 추도문을 수정하면 FlagAuthorizationException이 발생한다")
    void updateMemorial_ByNonWriter_ThrowsException() {
        // given
        FlagMemorial mockMemorial = mock(FlagMemorial.class);
        given(memorialRepository.findById(1L)).willReturn(Optional.of(mockMemorial));
        willThrow(new FlagAuthorizationException("후기 작성자만 접근 가능합니다."))
                .given(mockMemorial).updateContent(OTHER_ID, "수정 시도");

        // when / then
        assertThatThrownBy(() -> flagMemorialCommandService.updateMemorial(1L, OTHER_ID, "수정 시도"))
                .isInstanceOf(FlagAuthorizationException.class);
    }

    @Test
    @DisplayName("존재하지 않는 추도문을 수정하면 FlagMemorialNotFoundException이 발생한다")
    void updateMemorial_NotFound_ThrowsException() {
        // given
        given(memorialRepository.findById(999L)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> flagMemorialCommandService.updateMemorial(999L, WRITER_ID, "내용"))
                .isInstanceOf(FlagMemorialNotFoundException.class);
    }

    @Test
    @DisplayName("작성자가 추도문을 삭제할 수 있다")
    void deleteMemorial_ByWriter_Success() {
        // given
        FlagMemorial mockMemorial = mock(FlagMemorial.class);
        DeletableFlagMemorial mockTicket = mock(DeletableFlagMemorial.class);
        given(mockMemorial.getFlagId()).willReturn(FLAG_ID);
        given(memorialRepository.findById(1L)).willReturn(Optional.of(mockMemorial));
        given(mockMemorial.verifyForDeletion(WRITER_ID)).willReturn(mockTicket);

        // when
        flagMemorialCommandService.deleteMemorial(1L, WRITER_ID);

        // then
        verify(memorialRepository).delete(any(DeletableFlagMemorial.class));
        verify(eventPublisher).publishEvent(any(MemorialDeletedEvent.class));
    }

    @Test
    @DisplayName("작성자가 아닌 사용자가 추도문을 삭제하면 FlagAuthorizationException이 발생한다")
    void deleteMemorial_ByNonWriter_ThrowsException() {
        // given
        FlagMemorial mockMemorial = mock(FlagMemorial.class);
        given(memorialRepository.findById(1L)).willReturn(Optional.of(mockMemorial));
        willThrow(new FlagAuthorizationException("후기 작성자만 접근 가능합니다."))
                .given(mockMemorial).verifyForDeletion(OTHER_ID);

        // when / then
        assertThatThrownBy(() -> flagMemorialCommandService.deleteMemorial(1L, OTHER_ID))
                .isInstanceOf(FlagAuthorizationException.class);
    }

    @Test
    @DisplayName("존재하지 않는 추도문을 삭제하면 FlagMemorialNotFoundException이 발생한다")
    void deleteMemorial_NotFound_ThrowsException() {
        // given
        given(memorialRepository.findById(999L)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> flagMemorialCommandService.deleteMemorial(999L, WRITER_ID))
                .isInstanceOf(FlagMemorialNotFoundException.class);
    }
}
