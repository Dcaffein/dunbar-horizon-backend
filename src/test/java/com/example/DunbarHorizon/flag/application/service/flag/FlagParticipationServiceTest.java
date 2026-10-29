package com.example.DunbarHorizon.flag.application.service.flag;

import com.example.DunbarHorizon.flag.domain.flag.FlagParticipant;
import com.example.DunbarHorizon.flag.domain.exception.FlagErrorCode;
import com.example.DunbarHorizon.flag.domain.flag.FlagParticipationManager;
import com.example.DunbarHorizon.flag.domain.flag.FriendshipChecker;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagAuthorizationException;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagNotFoundException;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagParticipationDuplicateException;
import com.example.DunbarHorizon.flag.domain.flag.repository.FlagRepository;
import com.example.DunbarHorizon.global.exception.ErrorContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FlagParticipationServiceTest {

    @InjectMocks private FlagParticipationService flagParticipationService;

    @Mock private FlagRepository flagRepository;
    @Mock private FlagParticipationManager flagParticipationManager;
    @Mock private FriendshipChecker friendshipChecker;
    @Mock private PlatformTransactionManager transactionManager;

    private static final Long FLAG_ID = 1L;
    private static final Long USER_ID = 2L;
    private static final Long HOST_ID = 3L;

    @Test
    @DisplayName("호스트와의 친구 여부를 Manager에 넘기고 반환된 참여자를 저장한다")
    void participateInFlag_PassesFriendshipAndSavesParticipant() {
        // given
        FlagParticipant mockParticipant = mock(FlagParticipant.class);
        given(flagRepository.findHostIdById(FLAG_ID)).willReturn(Optional.of(HOST_ID));
        given(friendshipChecker.areFriends(HOST_ID, USER_ID)).willReturn(true);
        given(flagParticipationManager.participate(FLAG_ID, USER_ID, true)).willReturn(mockParticipant);

        // when
        flagParticipationService.participateInFlag(FLAG_ID, USER_ID);

        // then
        verify(flagParticipationManager).participate(FLAG_ID, USER_ID, true);
        verify(flagRepository).saveParticipant(mockParticipant);
    }

    @Test
    @DisplayName("호스트와 친구가 아니면 false를 그대로 Manager에 넘긴다")
    void participateInFlag_NotFriend_PassesFalseToManager() {
        // given
        given(flagRepository.findHostIdById(FLAG_ID)).willReturn(Optional.of(HOST_ID));
        given(friendshipChecker.areFriends(HOST_ID, USER_ID)).willReturn(false);
        willThrow(new FlagAuthorizationException(FlagErrorCode.FLAG_FRIENDS_ONLY, ErrorContext.of("flagId", FLAG_ID)))
                .given(flagParticipationManager).participate(FLAG_ID, USER_ID, false);

        // when / then
        assertThatThrownBy(() -> flagParticipationService.participateInFlag(FLAG_ID, USER_ID))
                .isInstanceOf(FlagAuthorizationException.class);
        verify(flagRepository, never()).saveParticipant(any());
    }

    @Test
    @DisplayName("플래그가 없으면 친구 확인 없이 FlagNotFoundException이 발생한다")
    void participateInFlag_FlagNotFound_ThrowsWithoutFriendshipCheck() {
        // given
        given(flagRepository.findHostIdById(FLAG_ID)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> flagParticipationService.participateInFlag(FLAG_ID, USER_ID))
                .isInstanceOf(FlagNotFoundException.class);
        verify(friendshipChecker, never()).areFriends(anyLong(), anyLong());
        verify(flagParticipationManager, never()).participate(anyLong(), anyLong(), anyBoolean());
    }

    @Test
    @DisplayName("Manager에서 FlagParticipationDuplicateException이 발생하면 서비스에서 그대로 전파된다")
    void participateInFlag_PolicyThrowsDuplicateException_Propagates() {
        // given
        given(flagRepository.findHostIdById(FLAG_ID)).willReturn(Optional.of(HOST_ID));
        given(friendshipChecker.areFriends(HOST_ID, USER_ID)).willReturn(true);
        willThrow(new FlagParticipationDuplicateException(FLAG_ID, USER_ID))
                .given(flagParticipationManager).participate(FLAG_ID, USER_ID, true);

        // when / then
        assertThatThrownBy(() -> flagParticipationService.participateInFlag(FLAG_ID, USER_ID))
                .isInstanceOf(FlagParticipationDuplicateException.class);
    }

    @Test
    @DisplayName("Manager가 반환한 FlagParticipant를 삭제한다")
    void leaveFlag_DeletesParticipantFromPolicy() {
        // given
        FlagParticipant mockParticipant = mock(FlagParticipant.class);
        given(flagParticipationManager.unparticipate(FLAG_ID, USER_ID)).willReturn(mockParticipant);

        // when
        flagParticipationService.leaveFlag(FLAG_ID, USER_ID);

        // then
        verify(flagParticipationManager).unparticipate(FLAG_ID, USER_ID);
        verify(flagRepository).deleteParticipant(mockParticipant);
    }
}
