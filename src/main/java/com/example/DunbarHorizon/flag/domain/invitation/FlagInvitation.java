package com.example.DunbarHorizon.flag.domain.invitation;

import com.example.DunbarHorizon.flag.domain.invitation.exception.FlagInvitationAccessException;
import com.example.DunbarHorizon.flag.domain.invitation.exception.FlagInvitationExpiredException;
import com.example.DunbarHorizon.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "flag_invitations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FlagInvitation extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long flagId;
    private Long inviterId;
    private Long inviteeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FlagInvitationStatus status;

    private LocalDateTime expiresAt;

    private FlagInvitation(Long flagId, Long inviterId, Long inviteeId, LocalDateTime expiresAt) {
        this.flagId = flagId;
        this.inviterId = inviterId;
        this.inviteeId = inviteeId;
        this.status = FlagInvitationStatus.PENDING;
        this.expiresAt = expiresAt;
    }

    public static FlagInvitation create(Long flagId, Long inviterId, Long inviteeId, LocalDateTime expiresAt) {
        return new FlagInvitation(flagId, inviterId, inviteeId, expiresAt);
    }

    public void accept(Long requesterId) {
        validateInvitee(requesterId);
        validatePending();
        validateNotExpired();
        this.status = FlagInvitationStatus.ACCEPTED;
    }

    public void reject(Long requesterId) {
        validateInvitee(requesterId);
        validatePending();
        this.status = FlagInvitationStatus.REJECTED;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isPending() {
        return status == FlagInvitationStatus.PENDING;
    }

    private void validateInvitee(Long requesterId) {
        if (!inviteeId.equals(requesterId)) {
            throw new FlagInvitationAccessException("초대받은 본인만 응답할 수 있습니다.");
        }
    }

    private void validatePending() {
        if (status != FlagInvitationStatus.PENDING) {
            throw new FlagInvitationAccessException("이미 처리된 초대장입니다.");
        }
    }

    private void validateNotExpired() {
        if (isExpired()) {
            throw new FlagInvitationExpiredException();
        }
    }
}
