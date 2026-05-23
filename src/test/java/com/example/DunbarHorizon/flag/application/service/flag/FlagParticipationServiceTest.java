package com.example.DunbarHorizon.flag.application.service.flag;

import com.example.DunbarHorizon.flag.domain.flag.FlagParticipant;
import com.example.DunbarHorizon.flag.domain.flag.FlagParticipationPolicy;
import com.example.DunbarHorizon.flag.domain.flag.repository.FlagParticipantRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FlagParticipationServiceTest {

    @InjectMocks private FlagParticipationService flagParticipationService;

    @Mock private FlagParticipantRepository participantRepository;
    @Mock private FlagParticipationPolicy flagParticipationPolicy;

    private static final Long FLAG_ID = 1L;
    private static final Long USER_ID = 2L;

    @Test
    @DisplayName("Policy가 반환한 참여자를 저장한다")
    void participateInFlag_SavesParticipantFromPolicy() {
        // given
        FlagParticipant mockParticipant = mock(FlagParticipant.class);
        given(flagParticipationPolicy.participate(FLAG_ID, USER_ID)).willReturn(mockParticipant);

        // when
        flagParticipationService.participateInFlag(FLAG_ID, USER_ID);

        // then
        verify(flagParticipationPolicy).participate(FLAG_ID, USER_ID);
        verify(participantRepository).save(mockParticipant);
    }

    @Test
    @DisplayName("Policy가 반환한 FlagParticipant를 삭제한다")
    void leaveFlag_DeletesParticipantFromPolicy() {
        // given
        FlagParticipant mockParticipant = mock(FlagParticipant.class);
        given(flagParticipationPolicy.unparticipate(FLAG_ID, USER_ID)).willReturn(mockParticipant);

        // when
        flagParticipationService.leaveFlag(FLAG_ID, USER_ID);

        // then
        verify(flagParticipationPolicy).unparticipate(FLAG_ID, USER_ID);
        verify(participantRepository).delete(mockParticipant);
    }
}
