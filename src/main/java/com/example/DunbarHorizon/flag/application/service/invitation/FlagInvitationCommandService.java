package com.example.DunbarHorizon.flag.application.service.invitation;

import com.example.DunbarHorizon.flag.application.port.in.FlagInvitationUseCase;
import com.example.DunbarHorizon.flag.domain.flag.Flag;
import com.example.DunbarHorizon.flag.domain.flag.FlagParticipant;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagNotFoundException;
import com.example.DunbarHorizon.flag.domain.flag.repository.FlagRepository;
import com.example.DunbarHorizon.flag.domain.invitation.FlagInvitation;
import com.example.DunbarHorizon.flag.domain.flag.FlagParticipationManager;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagParticipantNotFoundException;
import com.example.DunbarHorizon.flag.domain.invitation.event.FlagInvitationSentEvent;
import com.example.DunbarHorizon.flag.domain.invitation.exception.FlagInvitationDuplicateException;
import com.example.DunbarHorizon.flag.domain.invitation.exception.FlagInvitationNotFoundException;
import com.example.DunbarHorizon.flag.domain.invitation.repository.FlagInvitationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class FlagInvitationCommandService implements FlagInvitationUseCase {

    private final FlagParticipationManager participationManager;
    private final FlagInvitationRepository invitationRepository;
    private final FlagRepository flagRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void updateInvitePermission(Long flagId, Long requesterId, Long participantUserId, boolean canInvite) {
        Flag flag = flagRepository.findById(flagId)
                .orElseThrow(() -> new FlagNotFoundException(flagId));
        FlagParticipant participant = flagRepository.findParticipant(flagId, participantUserId)
                .orElseThrow(() -> new FlagParticipantNotFoundException(participantUserId));
        if (canInvite) {
            flag.grantInvitePermission(requesterId, participant);
        } else {
            flag.revokeInvitePermission(requesterId, participant);
        }
        flagRepository.saveParticipant(participant);
    }

    @Override
    public Long invite(Long flagId, Long inviterId, Long inviteeId) {
        Flag flag = flagRepository.findById(flagId)
                .orElseThrow(() -> new FlagNotFoundException(flagId));
        FlagParticipant inviter = flag.getHostId().equals(inviterId) ? null
                : flagRepository.findParticipant(flagId, inviterId).orElse(null);
        boolean inviteeParticipating = flagRepository.isParticipating(flagId, inviteeId);
        boolean alreadyInvited = invitationRepository.existsByFlagIdAndInviteeId(flagId, inviteeId);
        FlagInvitation invitation = FlagInvitation.create(
                flag, inviterId, inviteeId, inviter, inviteeParticipating, alreadyInvited);
        FlagInvitation saved = saveInvitation(invitation);

        eventPublisher.publishEvent(new FlagInvitationSentEvent(
                flagId, saved.getId(), inviteeId, flag.getTitle(), false
        ));

        return saved.getId();
    }

    @Override
    // 초대 조회가 만든 read view와 무관하게, Flag 락 뒤 최신 참여자 수로 정원을 확인해야 한다.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void accept(Long invitationId, Long requesterId) {
        FlagInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new FlagInvitationNotFoundException(invitationId));
        invitation.accept(requesterId);
        FlagParticipant newParticipant = participationManager.participateByInvitation(invitation.getFlagId(), requesterId);
        flagRepository.saveParticipant(newParticipant);
        invitationRepository.deleteById(invitationId);
    }

    @Override
    public void delete(Long invitationId, Long requesterId) {
        FlagInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new FlagInvitationNotFoundException(invitationId));

        invitation.delete(requesterId);
        invitationRepository.deleteById(invitationId);
    }

    private FlagInvitation saveInvitation(FlagInvitation invitation) {
        try {
            return invitationRepository.save(invitation);
        } catch (DataIntegrityViolationException e) {
            throw new FlagInvitationDuplicateException(invitation.getFlagId(), invitation.getInviteeId());
        }
    }
}
