package com.example.DunbarHorizon.buzz.application.port.out;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface BuzzSocialPort {
    Set<Long> getNonReceivableFriendIds(Long memberId);
    Set<Long> getFriendIdsIn(Long userId, Collection<Long> targetIds);
    Set<Long> getLabelMemberIds(Long ownerId, List<String> labelIds);
    Set<Long> getIntermediaryExpansionIds(Long userId, Long intermediaryFriendId, Double expansionValue);
}
