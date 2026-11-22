package com.example.DunbarHorizon.buzz.adapter.out.social;

import com.example.DunbarHorizon.account.application.port.in.UserQueryUseCase;
import com.example.DunbarHorizon.buzz.application.dto.info.BuzzCreatorInfo;
import com.example.DunbarHorizon.buzz.application.port.out.BuzzSocialPort;
import com.example.DunbarHorizon.social.application.friendship.port.FriendshipQueryUseCase;
import com.example.DunbarHorizon.social.application.label.port.LabelQueryUseCase;
import com.example.DunbarHorizon.social.application.network.dto.IntermediaryExpansionResult;
import com.example.DunbarHorizon.social.application.network.port.SocialExpansionQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BuzzSocialAdapter implements BuzzSocialPort {

    private final UserQueryUseCase userQueryUseCase;
    private final FriendshipQueryUseCase friendshipQueryUseCase;
    private final LabelQueryUseCase labelQueryUseCase;
    private final SocialExpansionQueryUseCase socialExpansionQueryUseCase;

    @Override
    public List<BuzzCreatorInfo> getCreatorProfiles(List<Long> userIds) {
        return userQueryUseCase.getUserProfiles(userIds).stream()
                .map(user -> new BuzzCreatorInfo(user.id(), user.nickname(), user.profileImage()))
                .toList();
    }

    @Override
    public Set<Long> getNonReceivableFriendIds(Long memberId) {
        return friendshipQueryUseCase.getMutedIds(memberId);
    }

    @Override
    public Set<Long> getFriendIdsIn(Long userId, Collection<Long> targetIds) {
        if (targetIds.isEmpty()) {
            return Set.of();
        }
        return friendshipQueryUseCase.getFriendIdsIn(userId, targetIds);
    }

    @Override
    public Set<Long> getLabelMemberIds(Long ownerId, List<String> labelIds) {
        return labelQueryUseCase.getMemberIdsInOwnedLabels(ownerId, labelIds);
    }

    @Override
    public Set<Long> getIntermediaryExpansionIds(Long userId, Long intermediaryFriendId, Double expansionValue) {
        return socialExpansionQueryUseCase.getIntermediaryExpansion(userId, intermediaryFriendId, expansionValue).stream()
                .map(IntermediaryExpansionResult::id)
                .collect(Collectors.toSet());
    }
}
