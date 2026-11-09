package com.example.DunbarHorizon.flag.domain.invitation;

import com.example.DunbarHorizon.support.FlagInvitationFixture;

import com.example.DunbarHorizon.flag.domain.invitation.exception.FlagInvitationAccessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.example.DunbarHorizon.flag.domain.flag.Flag;
import com.example.DunbarHorizon.flag.domain.flag.FlagParticipant;
import com.example.DunbarHorizon.flag.domain.flag.FlagSchedule;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagAuthorizationException;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagInvalidStatusException;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagParticipantNotFoundException;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagParticipationDuplicateException;
import com.example.DunbarHorizon.flag.domain.invitation.exception.FlagInvitationDuplicateException;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FlagInvitationTest {

    private static final Long FLAG_ID = 1L;
    private static final Long INVITER_ID = 2L;
    private static final Long INVITEE_ID = 3L;

    private static final Long HOST_ID = 1L;
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
    @DisplayName("초대받은 사람은 초대를 수락할 수 있다")
    void accept_ByInvitee_Succeeds() {
        // given
        FlagInvitation invitation = FlagInvitationFixture.invitation(FLAG_ID, INVITER_ID, INVITEE_ID);

        // when / then
        assertThatNoException().isThrownBy(() -> invitation.accept(INVITEE_ID));
    }

    @Test
    @DisplayName("피초대자의 삭제 요청은 거절로 처리한다")
    void delete_ByInvitee_Succeeds() {
        FlagInvitation invitation = FlagInvitationFixture.invitation(FLAG_ID, INVITER_ID, INVITEE_ID);

        assertThatNoException().isThrownBy(() -> invitation.delete(INVITEE_ID));
    }

    @Test
    @DisplayName("초대자의 삭제 요청은 취소로 처리한다")
    void delete_ByInviter_Succeeds() {
        FlagInvitation invitation = FlagInvitationFixture.invitation(FLAG_ID, INVITER_ID, INVITEE_ID);

        assertThatNoException().isThrownBy(() -> invitation.delete(INVITER_ID));
    }

    @Test
    @DisplayName("초대와 무관한 사용자는 삭제할 수 없다")
    void delete_ByThirdParty_Throws() {
        FlagInvitation invitation = FlagInvitationFixture.invitation(FLAG_ID, INVITER_ID, INVITEE_ID);

        assertThatThrownBy(() -> invitation.delete(99L))
                .isInstanceOf(FlagInvitationAccessException.class);
    }

    @Test
    @DisplayName("초대 권한이 있는 참여자가 친구를 초대할 수 있다")
    void invite_Success() {
        // given
        Flag flag = recruitingFlag();
        FlagParticipant inviter = inviterWithPermission();

        // when
        FlagInvitation result = FlagInvitation.create(flag, INVITER_ID, INVITEE_ID, inviter, false, false);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getInviteeId()).isEqualTo(INVITEE_ID);
    }

    @Test
    @DisplayName("호스트는 canInvite 없이도 초대할 수 있다")
    void invite_HostIsInviter_Success() {
        // given
        Flag flag = recruitingFlag();

        // when
        FlagInvitation result = FlagInvitation.create(flag, HOST_ID, INVITEE_ID, null, false, false);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getInviteeId()).isEqualTo(INVITEE_ID);
    }

    @Test
    @DisplayName("초대 권한이 없는 참여자가 초대하면 FlagAuthorizationException이 발생한다")
    void invite_InviterHasNoPermission_Throws() {
        // given
        Flag flag = recruitingFlag();
        FlagParticipant inviter = inviterWithoutPermission();

        // when / then
        assertThatThrownBy(() -> FlagInvitation.create(flag, INVITER_ID, INVITEE_ID, inviter, false, false))
                .isInstanceOf(FlagAuthorizationException.class);
    }

    @Test
    @DisplayName("동일 (flagId, inviteeId) 쌍의 PENDING 초대가 이미 있으면 FlagInvitationDuplicateException이 발생한다")
    void invite_DuplicatePending_Throws() {
        // given
        Flag flag = recruitingFlag();
        FlagParticipant inviter = inviterWithPermission();

        // when / then
        assertThatThrownBy(() -> FlagInvitation.create(flag, INVITER_ID, INVITEE_ID, inviter, false, true))
                .isInstanceOf(FlagInvitationDuplicateException.class);
    }

    @Test
    @DisplayName("호스트를 초대하면 FlagAuthorizationException이 발생한다")
    void invite_InviteeIsHost_Throws() {
        // given
        Flag flag = recruitingFlag();

        // when / then
        assertThatThrownBy(() -> FlagInvitation.create(flag, INVITER_ID, HOST_ID, null, false, false))
                .isInstanceOf(FlagAuthorizationException.class);
    }

    @Test
    @DisplayName("이미 참여 중인 사용자를 초대하면 FlagParticipationDuplicateException이 발생한다")
    void invite_InviteeAlreadyParticipating_Throws() {
        // given
        Flag flag = recruitingFlag();
        FlagParticipant inviter = inviterWithPermission();

        // when / then
        assertThatThrownBy(() -> FlagInvitation.create(flag, INVITER_ID, INVITEE_ID, inviter, true, false))
                .isInstanceOf(FlagParticipationDuplicateException.class);
    }

    @Test
    @DisplayName("모집이 종료되면 초대를 생성할 수 없다")
    void create_NotRecruiting_Throws() {
        Flag flag = recruitingFlag();
        flag.closeRecruitment(HOST_ID);
        assertThatThrownBy(() -> FlagInvitation.create(flag, HOST_ID, INVITEE_ID, null, false, false))
                .isInstanceOf(FlagInvalidStatusException.class);
    }

    @Test
    @DisplayName("호스트가 아닌 발신자의 참여 정보가 없으면 거절한다")
    void create_MissingInviter_Throws() {
        assertThatThrownBy(() -> FlagInvitation.create(recruitingFlag(), INVITER_ID, INVITEE_ID, null, false, false))
                .isInstanceOf(FlagParticipantNotFoundException.class);
    }
}
