package com.example.DunbarHorizon.flag.domain.flag;

import com.example.DunbarHorizon.flag.domain.flag.exception.FlagAuthorizationException;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagParticipationDuplicateException;
import com.example.DunbarHorizon.flag.domain.flag.repository.FlagParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FlagParticipationPolicy {
    private final FlagParticipantRepository flagParticipantRepository;
    private final FriendshipChecker friendshipChecker;

    public FlagParticipant participate(Flag flag, Long requesterId) {
        validateParticipationEligibility(flag, requesterId);

        int currentParticipantCount = flagParticipantRepository.countByFlagId(flag.getId());
        return flag.participate(requesterId, currentParticipantCount);
    }

    public void updateCapacity(Flag flag, Long requesterId, Integer newMaxCapacity) {
        int currentCount = flagParticipantRepository.countByFlagId(flag.getId());
        flag.updateCapacity(requesterId, newMaxCapacity, currentCount);
    }

    private void validateParticipationEligibility(Flag flag, Long requesterId) {
        if (flagParticipantRepository.isParticipating(flag.getId(), requesterId)) {
            throw new FlagParticipationDuplicateException(flag.getId(), requesterId);
        }

        if (!friendshipChecker.areFriends(flag.getHostId(), requesterId)) {
            throw new FlagAuthorizationException("호스트의 친구만 참여할 수 있는 플래그입니다.");
        }
    }
}
