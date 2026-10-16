package com.example.DunbarHorizon.social.domain.label;

import com.example.DunbarHorizon.social.domain.friend.exception.FriendshipNotFoundException;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendshipRepository;
import com.example.DunbarHorizon.social.domain.label.exception.NonFriendLabelMemberException;
import com.example.DunbarHorizon.social.domain.label.exception.DuplicateLabelMemberException;
import com.example.DunbarHorizon.social.domain.label.repository.LabelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class LabelMemberEnroller {
    private final FriendshipRepository friendshipRepository;
    private final LabelRepository labelRepository;

    public void addNewMember(String labelId, Long ownerId, Long newMemberId) {
        if (!friendshipRepository.existsFriendshipBetween(ownerId, newMemberId)) {
            throw new FriendshipNotFoundException(ownerId, newMemberId);
        }

        if (labelRepository.existsMember(labelId, newMemberId)) {
            throw new DuplicateLabelMemberException(newMemberId);
        }

        labelRepository.addMember(labelId, newMemberId);
    }

    public void updateMembers(String labelId, Long ownerId, List<Long> newMemberIds) {
        if (newMemberIds == null || newMemberIds.isEmpty()) {
            labelRepository.replaceMembers(labelId, Set.of());
            return;
        }

        List<Long> distinctIds = newMemberIds.stream().distinct().toList();

        Set<Long> validFriendIds = friendshipRepository.filterFriendIdsAmong(ownerId, distinctIds);

        if (validFriendIds.size() != distinctIds.size()) {
            List<Long> invalidIds = findInvalidIds(new HashSet<>(distinctIds), validFriendIds);
            throw new NonFriendLabelMemberException(invalidIds);
        }

        labelRepository.replaceMembers(labelId, validFriendIds);
    }

    private List<Long> findInvalidIds(Set<Long> requestedIds, Set<Long> validFriendIds) {
        return requestedIds.stream()
                .filter(id -> !validFriendIds.contains(id))
                .toList();
    }
}
