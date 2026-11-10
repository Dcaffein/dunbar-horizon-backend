package com.example.DunbarHorizon.flag.application.service.flag;

import com.example.DunbarHorizon.flag.domain.flag.exception.FlagAuthorizationException;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagInvalidStatusException;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagNotFoundException;
import com.example.DunbarHorizon.flag.domain.flag.repository.FlagRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import com.example.DunbarHorizon.flag.domain.flag.Flag;
import com.example.DunbarHorizon.flag.domain.flag.FlagSchedule;
import com.example.DunbarHorizon.flag.application.port.in.command.FlagEncoreCommand;
import com.example.DunbarHorizon.flag.application.port.in.command.FlagHostCommand;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FlagHostServiceTest {

    @InjectMocks private FlagHostService flagHostService;

    @Mock private FlagRepository flagRepository;

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
    @DisplayName("앵코르 생성은 부모를 읽어 앵코르만 저장하고 부모는 잠그거나 저장하지 않는다")
    void encore_SavesEncoreOnly() {
        // given
        Flag parent = endedParent();
        given(flagRepository.findById(PARENT_ID)).willReturn(Optional.of(parent));
        given(flagRepository.existsByParentId(PARENT_ID)).willReturn(false);
        given(flagRepository.save(any(Flag.class))).willAnswer(invocation -> {
            Flag saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 2L);
            return saved;
        });

        // when
        Long encoreId = flagHostService.encoreFlag(new FlagEncoreCommand(PARENT_ID, HOST_ID, DEADLINE, START, END));

        // then
        assertThat(encoreId).isEqualTo(2L);
        ArgumentCaptor<Flag> captor = ArgumentCaptor.forClass(Flag.class);
        verify(flagRepository).save(captor.capture());
        assertThat(captor.getValue().getParentId()).isEqualTo(PARENT_ID);
        verify(flagRepository, never()).findByIdForUpdate(any());
        assertThat(parent.isPreserved()).isFalse();
    }

    @Test
    @DisplayName("존재하지 않는 부모로 앵코르를 만들면 FlagNotFoundException이 발생한다")
    void encore_ParentNotFound_ThrowsException() {
        // given
        given(flagRepository.findById(PARENT_ID)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> flagHostService.encoreFlag(new FlagEncoreCommand(PARENT_ID, HOST_ID, DEADLINE, START, END)))
                .isInstanceOf(FlagNotFoundException.class);
    }

    @Test
    @DisplayName("호스트가 아닌 사용자가 앵코르를 만들면 FlagAuthorizationException이 발생한다")
    void encore_ByNonHost_ThrowsException() {
        // given
        given(flagRepository.findById(PARENT_ID)).willReturn(Optional.of(endedParent()));

        // when / then
        assertThatThrownBy(() -> flagHostService.encoreFlag(new FlagEncoreCommand(PARENT_ID, OTHER_ID, DEADLINE, START, END)))
                .isInstanceOf(FlagAuthorizationException.class);
        verify(flagRepository, never()).save(any());
    }

    @Test
    @DisplayName("이미 앵코르가 있는 부모에 앵코르를 만들면 FlagInvalidStatusException이 발생한다")
    void encore_AlreadyExists_ThrowsException() {
        // given
        given(flagRepository.findById(PARENT_ID)).willReturn(Optional.of(endedParent()));
        given(flagRepository.existsByParentId(PARENT_ID)).willReturn(true);

        // when / then
        assertThatThrownBy(() -> flagHostService.encoreFlag(new FlagEncoreCommand(PARENT_ID, HOST_ID, DEADLINE, START, END)))
                .isInstanceOf(FlagInvalidStatusException.class);
        verify(flagRepository, never()).save(any());
    }

    @Test
    @DisplayName("종료되지 않은 부모에는 앵코르를 만들 수 없다")
    void encore_ParentNotEnded_ThrowsException() {
        // given
        Flag recruiting = flagWith(FlagSchedule.of(NOW.plusHours(1), NOW.plusHours(2), NOW.plusHours(3)));
        given(flagRepository.findById(PARENT_ID)).willReturn(Optional.of(recruiting));
        given(flagRepository.existsByParentId(PARENT_ID)).willReturn(false);

        // when / then
        assertThatThrownBy(() -> flagHostService.encoreFlag(new FlagEncoreCommand(PARENT_ID, HOST_ID,
                NOW.plusHours(4), NOW.plusHours(5), NOW.plusHours(6))))
                .isInstanceOf(FlagInvalidStatusException.class);
        verify(flagRepository, never()).save(any());
    }
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
    @DisplayName("앵콜 저장의 유니크 제약 위반은 중복 앵콜 예외로 번역한다")
    void encoreFlag_Fail_UniqueConstraintViolation() {
        // given
        given(flagRepository.findById(PARENT_ID)).willReturn(Optional.of(endedParent()));
        given(flagRepository.save(any(Flag.class))).willThrow(new DataIntegrityViolationException("duplicate"));
        // when & then
        assertThatThrownBy(() -> flagHostService.encoreFlag(
                new FlagEncoreCommand(PARENT_ID, HOST_ID, DEADLINE, START, END)))
                .isInstanceOf(FlagInvalidStatusException.class);
    }
}
