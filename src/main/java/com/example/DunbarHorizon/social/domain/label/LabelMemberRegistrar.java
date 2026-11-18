package com.example.DunbarHorizon.social.domain.label;

import com.example.DunbarHorizon.social.domain.friendship.exception.FriendshipNotFoundException;
import com.example.DunbarHorizon.social.domain.label.exception.NonFriendLabelMemberException;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@Component
public class LabelMemberRegistrar {

    public void addMember(Label label, Long memberId, Set<Long> friendIds) {
        if (!friendIds.contains(memberId)) {
            throw new FriendshipNotFoundException(label.getOwnerId(), memberId);
        }
        label.addMember(memberId);
    }

    public void replaceMembers(Label label, Collection<Long> memberIds, Set<Long> friendIds) {
        List<Long> invalidIds = memberIds.stream().distinct().filter(id -> !friendIds.contains(id)).toList();
        if (!invalidIds.isEmpty()) {
            throw new NonFriendLabelMemberException(invalidIds);
        }
        label.replaceMembers(memberIds);
    }
}
