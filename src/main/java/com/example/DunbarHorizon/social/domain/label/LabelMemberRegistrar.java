package com.example.DunbarHorizon.social.domain.label;

import com.example.DunbarHorizon.social.domain.friend.exception.FriendshipNotFoundException;
import com.example.DunbarHorizon.social.domain.label.exception.NonFriendLabelMemberException;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@Component
public class LabelMemberRegistrar {

    public void addMember(Label label, Long memberId, Set<Long> friendIds) {
        if (!nonFriendIds(List.of(memberId), friendIds).isEmpty()) {
            throw new FriendshipNotFoundException(label.getOwnerId(), memberId);
        }
        label.addMember(memberId);
    }

    public void replaceMembers(Label label, Collection<Long> memberIds, Set<Long> friendIds) {
        List<Long> invalidIds = nonFriendIds(memberIds, friendIds);
        if (!invalidIds.isEmpty()) {
            throw new NonFriendLabelMemberException(invalidIds);
        }
        label.replaceMembers(memberIds);
    }

    private List<Long> nonFriendIds(Collection<Long> memberIds, Set<Long> friendIds) {
        return memberIds.stream().distinct().filter(id -> !friendIds.contains(id)).toList();
    }
}
