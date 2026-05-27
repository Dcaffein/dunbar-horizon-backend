package com.example.DunbarHorizon.flag.application.service.flag;

import com.example.DunbarHorizon.flag.application.port.in.command.FlagEncoreCommand;
import com.example.DunbarHorizon.flag.application.port.in.command.FlagHostCommand;
import com.example.DunbarHorizon.flag.domain.flag.Flag;
import com.example.DunbarHorizon.flag.domain.flag.FlagEncoreFactory;
import com.example.DunbarHorizon.flag.domain.flag.FlagSchedule;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagInvalidStatusException;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagNotFoundException;
import com.example.DunbarHorizon.flag.domain.flag.repository.FlagRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FlagHostServiceTest {

    @InjectMocks private FlagHostService flagHostService;

    @Mock private FlagRepository flagRepository;
    @Mock private FlagEncoreFactory flagEncoreFactory;

    private static final LocalDateTime NOW = LocalDateTime.now();

    private FlagHostCommand validHostCommand() {
        return new FlagHostCommand(
                1L, "제목", "설명", 10,
                NOW.plusHours(2), NOW.plusHours(3), NOW.plusHours(4)
        );
    }

    @Test
    @DisplayName("정상적인 커맨드로 플래그를 생성하고 저장된 플래그 ID를 반환한다")
    void hostFlag_Success() {
        // given
        FlagSchedule schedule = FlagSchedule.of(NOW.plusHours(2), NOW.plusHours(3), NOW.plusHours(4));
        Flag savedFlag = Flag.create(1L, "제목", "설명", 10, schedule);
        ReflectionTestUtils.setField(savedFlag, "id", 1L);
        given(flagRepository.save(any(Flag.class))).willReturn(savedFlag);

        // when
        Long result = flagHostService.hostFlag(validHostCommand());

        // then
        assertThat(result).isEqualTo(1L);
        verify(flagRepository).save(any(Flag.class));
    }

    @Test
    @DisplayName("존재하지 않는 부모 플래그로 앵코르를 생성하면 FlagNotFoundException이 발생한다")
    void encoreFlag_ParentNotFound_ThrowsException() {
        // given
        given(flagRepository.findById(99L)).willReturn(Optional.empty());
        FlagEncoreCommand command = new FlagEncoreCommand(99L, 1L, NOW.plusHours(2), NOW.plusHours(3), NOW.plusHours(4));

        // when / then
        assertThatThrownBy(() -> flagHostService.encoreFlag(command))
                .isInstanceOf(FlagNotFoundException.class);
    }

    @Test
    @DisplayName("동시 요청으로 DB unique 제약 위반 시 FlagInvalidStatusException이 발생한다")
    void encoreFlag_UniqueConstraintViolation_ThrowsException() {
        // given
        FlagSchedule parentSchedule = FlagSchedule.of(NOW.minusHours(3), NOW.minusHours(2), NOW.minusHours(1));
        Flag parentFlag = Flag.create(1L, "원본 플래그", "설명", 10, parentSchedule);
        ReflectionTestUtils.setField(parentFlag, "id", 1L);

        FlagSchedule encoreSchedule = FlagSchedule.of(NOW.plusHours(2), NOW.plusHours(3), NOW.plusHours(4));
        Flag encoreFlag = Flag.create(1L, "원본 플래그", "설명", 10, encoreSchedule);

        given(flagRepository.findById(1L)).willReturn(Optional.of(parentFlag));
        given(flagEncoreFactory.encore(any(), any(), any(), any(), any())).willReturn(encoreFlag);
        given(flagRepository.save(encoreFlag)).willThrow(new DataIntegrityViolationException("unique constraint violation"));

        FlagEncoreCommand command = new FlagEncoreCommand(1L, 1L, NOW.plusHours(2), NOW.plusHours(3), NOW.plusHours(4));

        // when / then
        assertThatThrownBy(() -> flagHostService.encoreFlag(command))
                .isInstanceOf(FlagInvalidStatusException.class);
    }

    @Test
    @DisplayName("정상적인 앵코르 생성 시 저장된 앵코르 플래그 ID를 반환한다")
    void encoreFlag_Success() {
        // given
        FlagSchedule parentSchedule = FlagSchedule.of(NOW.minusHours(3), NOW.minusHours(2), NOW.minusHours(1));
        Flag parentFlag = Flag.create(1L, "원본 플래그", "설명", 10, parentSchedule);
        ReflectionTestUtils.setField(parentFlag, "id", 1L);

        FlagSchedule encoreSchedule = FlagSchedule.of(NOW.plusHours(2), NOW.plusHours(3), NOW.plusHours(4));
        Flag encoreFlag = Flag.create(1L, "원본 플래그", "설명", 10, encoreSchedule);
        ReflectionTestUtils.setField(encoreFlag, "id", 2L);

        given(flagRepository.findById(1L)).willReturn(Optional.of(parentFlag));
        given(flagEncoreFactory.encore(any(), any(), any(), any(), any())).willReturn(encoreFlag);
        given(flagRepository.save(encoreFlag)).willReturn(encoreFlag);

        FlagEncoreCommand command = new FlagEncoreCommand(1L, 1L, NOW.plusHours(2), NOW.plusHours(3), NOW.plusHours(4));

        // when
        Long result = flagHostService.encoreFlag(command);

        // then
        assertThat(result).isEqualTo(2L);
        verify(flagEncoreFactory).encore(any(), any(), any(), any(), any());
    }
}
