package com.example.GooRoomBe.flag.domain.flag;

import com.example.GooRoomBe.global.common.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FlagParticipant extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long flagId;

    private Long participantId;

    FlagParticipant(Long flagId, Long participantId) {
        validate(flagId, participantId);
        this.flagId = flagId;
        this.participantId = participantId;
    }

    private void validate(Long flagId, Long participantId) {
        if (flagId == null || participantId == null) {
            throw new IllegalArgumentException("Flag ID와 참여자 ID는 필수입니다.");
        }
    }
}