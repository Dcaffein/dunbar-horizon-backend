package com.example.DunbarHorizon.flag.application.service.flag;

import com.example.DunbarHorizon.flag.application.port.in.command.FlagEncoreCommand;
import com.example.DunbarHorizon.flag.application.port.in.command.FlagHostCommand;
import com.example.DunbarHorizon.flag.domain.flag.Flag;
import com.example.DunbarHorizon.flag.domain.flag.FlagEncoreManager;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FlagHostServiceTest {

    @InjectMocks private FlagHostService flagHostService;

    @Mock private FlagRepository flagRepository;
    @Mock private FlagEncoreManager encoreManager;

    private static final LocalDateTime NOW = LocalDateTime.now();

    private FlagHostCommand validHostCommand() {
        return new FlagHostCommand(
                1L, "제목", "설명", 10,
                NOW.plusHours(2), NOW.plusHours(3), NOW.plusHours(4)
        );
    }

    private FlagEncoreCommand encoreCommand(Long parentFlagId) {
        return new FlagEncoreCommand(parentFlagId, 1L, NOW.plusHours(2), NOW.plusHours(3), NOW.plusHours(4));
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
    @DisplayName("앵코르 생성은 FlagEncoreManager에 위임하고 생성된 앵코르 ID를 반환한다")
    void encoreFlag_DelegatesAndReturnsId() {
        // given
        FlagEncoreCommand command = encoreCommand(1L);
        Flag encoreFlag = Flag.create(1L, "원본 플래그", "설명", 10,
                FlagSchedule.of(NOW.plusHours(2), NOW.plusHours(3), NOW.plusHours(4)));
        ReflectionTestUtils.setField(encoreFlag, "id", 2L);
        given(encoreManager.encore(1L, 1L, command.deadline(), command.startDateTime(), command.endDateTime()))
                .willReturn(encoreFlag);

        // when
        Long result = flagHostService.encoreFlag(command);

        // then
        assertThat(result).isEqualTo(2L);
    }

    @Test
    @DisplayName("존재하지 않는 부모 플래그로 앵코르를 생성하면 FlagNotFoundException이 발생한다")
    void encoreFlag_ParentNotFound_ThrowsException() {
        // given
        given(encoreManager.encore(any(), any(), any(), any(), any())).willThrow(new FlagNotFoundException(99L));

        // when / then
        assertThatThrownBy(() -> flagHostService.encoreFlag(encoreCommand(99L)))
                .isInstanceOf(FlagNotFoundException.class);
    }

    @Test
    @DisplayName("DB unique 제약 위반은 FlagInvalidStatusException으로 바꾼다")
    void encoreFlag_UniqueConstraintViolation_ThrowsException() {
        // given
        given(encoreManager.encore(any(), any(), any(), any(), any()))
                .willThrow(new DataIntegrityViolationException("unique constraint violation"));

        // when / then
        assertThatThrownBy(() -> flagHostService.encoreFlag(encoreCommand(1L)))
                .isInstanceOf(FlagInvalidStatusException.class);
    }
}
