package com.example.DunbarHorizon.flag.domain.flag;

import com.example.DunbarHorizon.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "flag_participants")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FlagParticipant extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long flagId;

    private Long participantId;

    @Column(nullable = false)
    private boolean canInvite = false;

    FlagParticipant(Long flagId, Long participantId) {
        validate(flagId, participantId);
        this.flagId = flagId;
        this.participantId = participantId;
    }

    public void grantInvitePermission() {
        this.canInvite = true;
    }

    public void revokeInvitePermission() {
        this.canInvite = false;
    }

    private void validate(Long flagId, Long participantId) {
        if (flagId == null || participantId == null) {
            throw new IllegalArgumentException("Flag ID와 참여자 ID는 필수입니다.");
        }
    }
}