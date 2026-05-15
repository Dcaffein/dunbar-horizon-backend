package com.example.DunbarHorizon.flag.domain.flag;

import com.example.DunbarHorizon.flag.domain.flag.exception.FlagAuthorizationException;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagNotFoundException;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagParticipationDuplicateException;
import com.example.DunbarHorizon.flag.domain.flag.repository.FlagParticipantRepository;
import com.example.DunbarHorizon.flag.domain.flag.repository.FlagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FlagParticipationPolicy {
    private final FlagParticipantRepository flagParticipantRepository;
    private final FlagRepository flagRepository;
    private final FriendshipChecker friendshipChecker;

    public FlagParticipant participate(Long flagId, Long requesterId) {
        Flag flagForCheck = flagRepository.findById(flagId)
                .orElseThrow(() -> new FlagNotFoundException(flagId));

        if (!friendshipChecker.areFriends(flagForCheck.getHostId(), requesterId)) {
            throw new FlagAuthorizationException("호스트의 친구만 참여할 수 있는 플래그입니다.");
        }

        Flag flag = flagRepository.findByIdExclusive(flagId)
                .orElseThrow(() -> new FlagNotFoundException(flagId));

        if (flagParticipantRepository.isParticipating(flagId, requesterId)) {
            throw new FlagParticipationDuplicateException(flagId, requesterId);
        }

        int currentCount = flagParticipantRepository.countByFlagId(flagId);
        return flag.participate(requesterId, currentCount);
    }

    public void updateCapacity(Flag flag, Long requesterId, Integer newMaxCapacity) {
        int currentCount = flagParticipantRepository.countByFlagId(flag.getId());
        flag.updateCapacity(requesterId, newMaxCapacity, currentCount);
    }
}
