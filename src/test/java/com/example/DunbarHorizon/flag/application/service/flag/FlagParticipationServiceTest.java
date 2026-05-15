package com.example.DunbarHorizon.flag.application.service.flag;

import com.example.DunbarHorizon.flag.domain.flag.DeletableParticipant;
import com.example.DunbarHorizon.flag.domain.flag.Flag;
import com.example.DunbarHorizon.flag.domain.flag.FlagParticipant;
import com.example.DunbarHorizon.flag.domain.flag.FlagParticipationPolicy;
import com.example.DunbarHorizon.flag.domain.flag.FlagSchedule;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagNotFoundException;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagParticipantNotFoundException;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagParticipationDuplicateException;
import com.example.DunbarHorizon.flag.domain.flag.repository.FlagParticipantRepository;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FlagParticipationServiceTest {

    @InjectMocks private FlagParticipationService flagParticipationService;

    @Mock private FlagRepository flagRepository;
    @Mock private FlagParticipantRepository participantRepository;
    @Mock private FlagParticipationPolicy flagParticipationPolicy;

    private static final Long FLAG_ID = 1L;
    private static final Long HOST_ID = 1L;
    private static final Long USER_ID = 2L;
    private static final LocalDateTime NOW = LocalDateTime.now();

    private Flag recruitingFlag() {
        FlagSchedule schedule = FlagSchedule.of(NOW.plusHours(2), NOW.plusHours(3), NOW.plusHours(4));
        Flag flag = Flag.create(HOST_ID, "테스트 플래그", "설명", 10, schedule);
        ReflectionTestUtils.setField(flag, "id", FLAG_ID);
        return flag;
    }

    @Test
    @DisplayName("정상적으로 플래그에 참여할 수 있다")
    void participateInFlag_Success() {
        // given
        FlagParticipant mockParticipant = mock(FlagParticipant.class);
        given(flagParticipationPolicy.participate(FLAG_ID, USER_ID)).willReturn(mockParticipant);

        // when
        flagParticipationService.participateInFlag(FLAG_ID, USER_ID);

        // then
        verify(participantRepository).save(mockParticipant);
    }

    @Test
    @DisplayName("Policy에서 FlagNotFoundException이 발생하면 서비스에서 그대로 전파된다")
    void participateInFlag_PolicyThrowsFlagNotFoundException_Propagates() {
        // given
        willThrow(new FlagNotFoundException(FLAG_ID))
                .given(flagParticipationPolicy).participate(FLAG_ID, USER_ID);

        // when / then
        assertThatThrownBy(() -> flagParticipationService.participateInFlag(FLAG_ID, USER_ID))
                .isInstanceOf(FlagNotFoundException.class);
    }

    @Test
    @DisplayName("Policy에서 FlagParticipationDuplicateException이 발생하면 서비스에서 그대로 전파된다")
    void participateInFlag_PolicyThrowsDuplicateException_Propagates() {
        // given
        willThrow(new FlagParticipationDuplicateException(FLAG_ID, USER_ID))
                .given(flagParticipationPolicy).participate(FLAG_ID, USER_ID);

        // when / then
        assertThatThrownBy(() -> flagParticipationService.participateInFlag(FLAG_ID, USER_ID))
                .isInstanceOf(FlagParticipationDuplicateException.class);
    }

    @Test
    @DisplayName("정상적으로 플래그 참여를 취소할 수 있다")
    void leaveFlag_Success() {
        // given
        Flag flag = recruitingFlag();
        FlagParticipant mockParticipant = mock(FlagParticipant.class);
        given(mockParticipant.getParticipantId()).willReturn(USER_ID);

        given(flagRepository.findById(FLAG_ID)).willReturn(Optional.of(flag));
        given(participantRepository.findByFlagIdAndParticipantId(FLAG_ID, USER_ID))
                .willReturn(Optional.of(mockParticipant));

        // when
        flagParticipationService.leaveFlag(FLAG_ID, USER_ID);

        // then
        verify(participantRepository).delete(any(DeletableParticipant.class));
    }

    @Test
    @DisplayName("참여하지 않은 플래그를 탈퇴하면 FlagParticipantNotFoundException이 발생한다")
    void leaveFlag_ParticipantNotFound_ThrowsException() {
        // given
        Flag flag = recruitingFlag();
        given(flagRepository.findById(FLAG_ID)).willReturn(Optional.of(flag));
        given(participantRepository.findByFlagIdAndParticipantId(FLAG_ID, USER_ID))
                .willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> flagParticipationService.leaveFlag(FLAG_ID, USER_ID))
                .isInstanceOf(FlagParticipantNotFoundException.class);
    }
}
