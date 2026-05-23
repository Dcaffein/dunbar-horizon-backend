package com.example.DunbarHorizon.flag.domain.flag;

import com.example.DunbarHorizon.flag.domain.flag.exception.FlagAuthorizationException;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagNotFoundException;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagParticipantNotFoundException;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagParticipationDuplicateException;
import com.example.DunbarHorizon.flag.domain.flag.repository.FlagParticipantRepository;
import com.example.DunbarHorizon.flag.domain.flag.repository.FlagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FlagParticipationPolicy {
    private final FlagRepository flagRepository;
    private final FlagParticipantRepository flagParticipantRepository;
    private final FriendshipChecker friendshipChecker;

    public FlagParticipant participate(Long flagId, Long userId) {
        Flag flag = flagRepository.findById(flagId)
                .orElseThrow(() -> new FlagNotFoundException(flagId));

        if (!friendshipChecker.areFriends(flag.getHostId(), userId)) {
            throw new FlagAuthorizationException("호스트의 친구만 참여할 수 있는 플래그입니다.");
        }

        Flag lockedFlag = flagRepository.findByIdExclusive(flagId)
                .orElseThrow(() -> new FlagNotFoundException(flagId));

        if (flagParticipantRepository.isParticipating(flagId, userId)) {
            throw new FlagParticipationDuplicateException(flagId, userId);
        }

        int count = flagParticipantRepository.countByFlagId(flagId);
        return lockedFlag.participate(userId, count);
    }

    public FlagParticipant unparticipate(Long flagId, Long userId) {
        Flag flag = flagRepository.findById(flagId)
                .orElseThrow(() -> new FlagNotFoundException(flagId));
        FlagParticipant participant = flagParticipantRepository
                .findByFlagIdAndParticipantId(flagId, userId)
                .orElseThrow(() -> new FlagParticipantNotFoundException(userId));
        flag.unparticipate(participant, userId);
        return participant;
    }
}
