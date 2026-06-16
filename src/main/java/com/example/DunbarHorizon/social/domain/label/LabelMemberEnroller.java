package com.example.DunbarHorizon.social.domain.label;

import com.example.DunbarHorizon.social.domain.friend.exception.FriendshipNotFoundException;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendshipRepository;
import com.example.DunbarHorizon.social.domain.label.exception.NonFriendLabelMemberException;
import com.example.DunbarHorizon.social.domain.label.exception.DuplicateLabelMemberException;
import com.example.DunbarHorizon.social.domain.socialUser.exception.UserReferenceNotFoundException;
import com.example.DunbarHorizon.social.domain.socialUser.repository.SocialUserRepository;
import com.example.DunbarHorizon.social.domain.socialUser.UserReference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class LabelMemberEnroller {
    private final FriendshipRepository friendshipRepository;
    private final SocialUserRepository socialUserRepository;

    public void addNewMember(Label label, Long newMemberId) {
        if (!friendshipRepository.existsFriendshipBetween(label.getOwner().getId(), newMemberId)) {
            throw new FriendshipNotFoundException(label.getOwner().getId(), newMemberId);
        }

        if (label.getMembers().stream().anyMatch(m -> m.getId().equals(newMemberId))) {
            throw new DuplicateLabelMemberException(newMemberId);
        }

        UserReference newMember = socialUserRepository.findById(newMemberId)
                .orElseThrow(() -> new UserReferenceNotFoundException(newMemberId));
        label.addNewMember(newMember);
    }

    public void updateMembers(Label label, List<Long> newMemberIds) {
        if (newMemberIds == null || newMemberIds.isEmpty()) {
            label.updateMembers(Set.of());
            return;
        }

        List<Long> distinctIds = newMemberIds.stream().distinct().toList();
        Long ownerId = label.getOwner().getId();

        Set<Long> validFriendIds = friendshipRepository.filterFriendIdsAmong(ownerId, distinctIds);

        if (validFriendIds.size() != distinctIds.size()) {
            List<Long> invalidIds = findInvalidIds(new HashSet<>(distinctIds), validFriendIds);
            throw new NonFriendLabelMemberException(invalidIds);
        }

        Set<UserReference> validMembers = socialUserRepository.findAllUserReferencesById(validFriendIds);
        label.updateMembers(validMembers);
    }

    private List<Long> findInvalidIds(Set<Long> requestedIds, Set<Long> validFriendIds) {
        return requestedIds.stream()
                .filter(id -> !validFriendIds.contains(id))
                .toList();
    }
}
