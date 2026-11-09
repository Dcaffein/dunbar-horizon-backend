package com.example.DunbarHorizon.flag.application.service.invitation;

import com.example.DunbarHorizon.flag.domain.flag.Flag;
import com.example.DunbarHorizon.flag.domain.flag.FlagParticipant;
import com.example.DunbarHorizon.flag.domain.flag.FlagParticipationManager;
import com.example.DunbarHorizon.flag.domain.flag.FlagSchedule;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagAuthorizationException;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagDeadlinePassedException;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagParticipationDuplicateException;
import com.example.DunbarHorizon.flag.domain.flag.repository.FlagRepository;
import com.example.DunbarHorizon.flag.domain.invitation.FlagInvitation;
import com.example.DunbarHorizon.flag.domain.invitation.event.FlagInvitationSentEvent;
import com.example.DunbarHorizon.flag.domain.invitation.exception.FlagInvitationAccessException;
import com.example.DunbarHorizon.flag.domain.invitation.exception.FlagInvitationDuplicateException;
import com.example.DunbarHorizon.flag.domain.invitation.repository.FlagInvitationRepository;
import com.example.DunbarHorizon.support.FlagInvitationFixture;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlagInvitationCommandServiceTest {

    @InjectMocks private FlagInvitationCommandService service;
    @Mock private ApplicationEventPublisher eventPublisher;

    @Mock private FlagRepository flagRepository;
    @Mock private FlagInvitationRepository invitationRepository;
    @Mock private FlagParticipationManager flagParticipationManager;

    private static final Long FLAG_ID = 1L;
    private static final Long HOST_ID = 1L;
    private static final Long INVITER_ID = 2L;
    private static final Long INVITEE_ID = 3L;
    private static final LocalDateTime NOW = LocalDateTime.now();

    private Flag recruitingFlag() {
        FlagSchedule schedule = FlagSchedule.of(NOW.plusHours(2), NOW.plusHours(3), NOW.plusHours(4));
        Flag flag = Flag.create(HOST_ID, "테스트 플래그", "설명", 10, schedule);
        ReflectionTestUtils.setField(flag, "id", FLAG_ID);
        return flag;
    }

    private FlagParticipant inviterWithPermission() {
        FlagParticipant participant = mock(FlagParticipant.class);
        lenient().when(participant.isCanInvite()).thenReturn(true);
        return participant;
    }

    private FlagParticipant inviterWithoutPermission() {
        FlagParticipant participant = mock(FlagParticipant.class);
        lenient().when(participant.isCanInvite()).thenReturn(false);
        return participant;
    }

    @Test
    @DisplayName("호스트가 참여자에게 초대 권한을 부여할 수 있다")
    void updateInvitePermission_Grant_Success() {
        // given
        Flag flag = recruitingFlag();
        FlagParticipant participant = inviterWithoutPermission();

        given(flagRepository.findById(FLAG_ID)).willReturn(Optional.of(flag));
        given(flagRepository.findParticipant(FLAG_ID, INVITER_ID)).willReturn(Optional.of(participant));

        // when
        service.updateInvitePermission(FLAG_ID, HOST_ID, INVITER_ID, true);

        // then
        verify(participant).grantInvitePermission();
        verify(flagRepository).saveParticipant(participant);
    }

    @Test
    @DisplayName("호스트가 아닌 사용자가 권한 변경을 시도하면 FlagAuthorizationException이 발생한다")
    void updateInvitePermission_NotHost_Throws() {
        // given
        Flag flag = recruitingFlag();
        FlagParticipant participant = inviterWithoutPermission();

        given(flagRepository.findById(FLAG_ID)).willReturn(Optional.of(flag));
        given(flagRepository.findParticipant(FLAG_ID, INVITER_ID)).willReturn(Optional.of(participant));

        // when / then
        assertThatThrownBy(() -> service.updateInvitePermission(FLAG_ID, INVITER_ID, INVITER_ID, true))
                .isInstanceOf(FlagAuthorizationException.class);
    }

    @Test
    @DisplayName("초대 권한이 있는 참여자가 친구를 초대할 수 있다")
    void invite_Success() {
        // given
        Flag flag = recruitingFlag();
        FlagParticipant inviter = inviterWithPermission();

        given(flagRepository.findById(FLAG_ID)).willReturn(Optional.of(flag));
        given(flagRepository.findParticipant(FLAG_ID, INVITER_ID)).willReturn(Optional.of(inviter));
        given(flagRepository.isParticipating(FLAG_ID, INVITEE_ID)).willReturn(false);
        given(invitationRepository.existsByFlagIdAndInviteeId(FLAG_ID, INVITEE_ID)).willReturn(false);

        given(invitationRepository.save(any(FlagInvitation.class))).willAnswer(call -> {
            FlagInvitation saved = call.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 10L);
            return saved;
        });

        // when
        Long result = service.invite(FLAG_ID, INVITER_ID, INVITEE_ID);

        // then
        assertThat(result).isEqualTo(10L);
        verify(eventPublisher).publishEvent(new FlagInvitationSentEvent(
                FLAG_ID, 10L, INVITEE_ID, flag.getTitle(), false));
    }

    @Test
    @DisplayName("호스트는 canInvite 없이도 초대할 수 있다")
    void invite_HostIsInviter_Success() {
        // given
        Flag flag = recruitingFlag();

        given(flagRepository.findById(FLAG_ID)).willReturn(Optional.of(flag));
        given(flagRepository.isParticipating(FLAG_ID, INVITEE_ID)).willReturn(false);
        given(invitationRepository.existsByFlagIdAndInviteeId(FLAG_ID, INVITEE_ID)).willReturn(false);

        given(invitationRepository.save(any(FlagInvitation.class))).willAnswer(call -> {
            FlagInvitation saved = call.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 10L);
            return saved;
        });

        // when
        Long result = service.invite(FLAG_ID, HOST_ID, INVITEE_ID);

        // then
        assertThat(result).isEqualTo(10L);
        verify(eventPublisher).publishEvent(new FlagInvitationSentEvent(
                FLAG_ID, 10L, INVITEE_ID, flag.getTitle(), false));
    }

    @Test
    @DisplayName("초대 권한이 없는 참여자가 초대하면 FlagAuthorizationException이 발생한다")
    void invite_InviterHasNoPermission_Throws() {
        // given
        Flag flag = recruitingFlag();
        FlagParticipant inviter = inviterWithoutPermission();

        given(flagRepository.findById(FLAG_ID)).willReturn(Optional.of(flag));
        given(flagRepository.findParticipant(FLAG_ID, INVITER_ID)).willReturn(Optional.of(inviter));

        // when / then
        assertThatThrownBy(() -> service.invite(FLAG_ID, INVITER_ID, INVITEE_ID))
                .isInstanceOf(FlagAuthorizationException.class);
    }

    @Test
    @DisplayName("동일 (flagId, inviteeId) 쌍의 PENDING 초대가 이미 있으면 FlagInvitationDuplicateException이 발생한다")
    void invite_DuplicatePending_Throws() {
        // given
        Flag flag = recruitingFlag();
        FlagParticipant inviter = inviterWithPermission();

        given(flagRepository.findById(FLAG_ID)).willReturn(Optional.of(flag));
        given(flagRepository.findParticipant(FLAG_ID, INVITER_ID)).willReturn(Optional.of(inviter));
        given(flagRepository.isParticipating(FLAG_ID, INVITEE_ID)).willReturn(false);
        given(invitationRepository.existsByFlagIdAndInviteeId(FLAG_ID, INVITEE_ID)).willReturn(true);

        // when / then
        assertThatThrownBy(() -> service.invite(FLAG_ID, INVITER_ID, INVITEE_ID))
                .isInstanceOf(FlagInvitationDuplicateException.class);
    }

    @Test
    @DisplayName("호스트를 초대하면 FlagAuthorizationException이 발생한다")
    void invite_InviteeIsHost_Throws() {
        // given
        Flag flag = recruitingFlag();

        given(flagRepository.findById(FLAG_ID)).willReturn(Optional.of(flag));

        // when / then
        assertThatThrownBy(() -> service.invite(FLAG_ID, INVITER_ID, HOST_ID))
                .isInstanceOf(FlagAuthorizationException.class);
    }

    @Test
    @DisplayName("이미 참여 중인 사용자를 초대하면 FlagParticipationDuplicateException이 발생한다")
    void invite_InviteeAlreadyParticipating_Throws() {
        // given
        Flag flag = recruitingFlag();
        FlagParticipant inviter = inviterWithPermission();

        given(flagRepository.findById(FLAG_ID)).willReturn(Optional.of(flag));
        given(flagRepository.findParticipant(FLAG_ID, INVITER_ID)).willReturn(Optional.of(inviter));
        given(flagRepository.isParticipating(FLAG_ID, INVITEE_ID)).willReturn(true);

        // when / then
        assertThatThrownBy(() -> service.invite(FLAG_ID, INVITER_ID, INVITEE_ID))
                .isInstanceOf(FlagParticipationDuplicateException.class);
    }

    @Test
    @DisplayName("초대받은 사람이 수락하면 FlagParticipant가 생성된다")
    void accept_Success() {
        // given
        FlagInvitation invitation = FlagInvitationFixture.invitation(FLAG_ID, INVITER_ID, INVITEE_ID);
        ReflectionTestUtils.setField(invitation, "id", 10L);

        FlagParticipant participant = mock(FlagParticipant.class);

        given(invitationRepository.findById(10L)).willReturn(Optional.of(invitation));
        given(flagParticipationManager.participateByInvitation(FLAG_ID, INVITEE_ID)).willReturn(participant);

        // when
        service.accept(10L, INVITEE_ID);

        // then
        var order = inOrder(flagParticipationManager, flagRepository, invitationRepository);
        order.verify(flagParticipationManager).participateByInvitation(FLAG_ID, INVITEE_ID);
        order.verify(flagRepository).saveParticipant(participant);
        order.verify(invitationRepository).deleteById(10L);
    }

    @Test
    @DisplayName("초대 수락 시 이미 참여 중이면 FlagParticipationDuplicateException이 발생한다")
    void accept_AlreadyParticipating_Throws() {
        // given
        FlagInvitation invitation = FlagInvitationFixture.invitation(FLAG_ID, INVITER_ID, INVITEE_ID);
        ReflectionTestUtils.setField(invitation, "id", 10L);

        given(invitationRepository.findById(10L)).willReturn(Optional.of(invitation));
        given(flagParticipationManager.participateByInvitation(FLAG_ID, INVITEE_ID))
                .willThrow(new FlagParticipationDuplicateException(FLAG_ID, INVITEE_ID));

        // when / then
        assertThatThrownBy(() -> service.accept(10L, INVITEE_ID))
                .isInstanceOf(FlagParticipationDuplicateException.class);
    }

    @Test
    @DisplayName("모집이 끝난 Flag의 초대를 수락하면 FlagDeadlinePassedException이 전파된다")
    void accept_RecruitmentClosed_Throws() {
        // given — 만료 판정은 Flag.participate()의 isRecruiting()이 수행한다
        FlagInvitation invitation = FlagInvitationFixture.invitation(FLAG_ID, INVITER_ID, INVITEE_ID);
        ReflectionTestUtils.setField(invitation, "id", 10L);

        given(invitationRepository.findById(10L)).willReturn(Optional.of(invitation));
        given(flagParticipationManager.participateByInvitation(FLAG_ID, INVITEE_ID))
                .willThrow(new FlagDeadlinePassedException(1L, null));

        // when / then
        assertThatThrownBy(() -> service.accept(10L, INVITEE_ID))
                .isInstanceOf(FlagDeadlinePassedException.class);
    }

    @Test
    @DisplayName("초대받지 않은 사람이 수락하면 FlagInvitationAccessException이 발생한다")
    void accept_NotInvitee_Throws() {
        // given
        FlagInvitation invitation = FlagInvitationFixture.invitation(FLAG_ID, INVITER_ID, INVITEE_ID);
        ReflectionTestUtils.setField(invitation, "id", 10L);

        given(invitationRepository.findById(10L)).willReturn(Optional.of(invitation));

        // when / then
        Long otherId = 99L;
        assertThatThrownBy(() -> service.accept(10L, otherId))
                .isInstanceOf(FlagInvitationAccessException.class);
        verifyNoInteractions(flagParticipationManager);
        verify(invitationRepository, never()).deleteById(any());
    }


    @Test
    @DisplayName("초대 저장의 유니크 제약 위반은 중복 예외로 번역한다")
    void invite_ConstraintViolation_ThrowsDuplicate() {
        given(flagRepository.findById(FLAG_ID)).willReturn(Optional.of(recruitingFlag()));
        given(invitationRepository.save(any(FlagInvitation.class)))
                .willThrow(new DataIntegrityViolationException("duplicate"));
        assertThatThrownBy(() -> service.invite(FLAG_ID, HOST_ID, INVITEE_ID))
                .isInstanceOf(FlagInvitationDuplicateException.class);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("초대 삭제 권한을 확인하고 삭제한다")
    void delete_Success() {
        FlagInvitation invitation = FlagInvitationFixture.invitation(FLAG_ID, INVITER_ID, INVITEE_ID);
        given(invitationRepository.findById(10L)).willReturn(Optional.of(invitation));
        service.delete(10L, INVITER_ID);
        verify(invitationRepository).deleteById(10L);
    }

    @Test
    @DisplayName("호스트는 참여자의 초대 권한을 회수할 수 있다")
    void updateInvitePermission_Revoke_Success() {
        FlagParticipant participant = inviterWithPermission();
        given(flagRepository.findById(FLAG_ID)).willReturn(Optional.of(recruitingFlag()));
        given(flagRepository.findParticipant(FLAG_ID, INVITER_ID)).willReturn(Optional.of(participant));
        service.updateInvitePermission(FLAG_ID, HOST_ID, INVITER_ID, false);
        verify(participant).revokeInvitePermission();
        verify(flagRepository).saveParticipant(participant);
    }
}
