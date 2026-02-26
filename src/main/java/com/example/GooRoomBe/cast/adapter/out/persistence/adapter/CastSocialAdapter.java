package com.example.GooRoomBe.cast.adapter.out.persistence.adapter;

import com.example.GooRoomBe.account.application.port.in.UserQueryUseCase;
import com.example.GooRoomBe.cast.application.port.out.CastSocialPort;
import com.example.GooRoomBe.cast.application.port.out.dto.CastCreatorDto;
import com.example.GooRoomBe.social.application.port.in.FriendshipQueryUseCase;
import com.example.GooRoomBe.social.application.port.in.LabelQueryUseCase;
import com.example.GooRoomBe.social.application.port.in.SocialNetworkQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class CastSocialAdapter implements CastSocialPort {
    private final UserQueryUseCase userQueryUseCase;
    private final FriendshipQueryUseCase friendshipQueryUseCase;
    private final LabelQueryUseCase labelQueryUseCase;
    private final SocialNetworkQueryUseCase networkQueryUseCase;

    @Override
    public Set<Long> getMemberIdsByLabels(Long creatorId, List<String> labelIds) {
        return labelQueryUseCase.getMemberIdsByLabels(creatorId, labelIds);
    }

    @Override
    public Set<Long> filterOnlyFriends(Long creatorId, List<Long> targetIds) {
        return friendshipQueryUseCase.getFriendIdsIn(creatorId, targetIds);
    }

    @Override
    public Set<Long> getPivotRecipientIds(Long creatorId, Long pivotFriendId, Double expansionValue) {
        return networkQueryUseCase.getPivotExpansion(creatorId, pivotFriendId, expansionValue );
    }

    @Override
    public List<CastCreatorDto> getCreatorProfiles(List<Long> userIds) {
        return userQueryUseCase.getUserProfiles(userIds).stream()
                .map(user -> new CastCreatorDto(
                        user.id(),
                        user.nickname(),
                        user.profileImage()
                ))
                .toList();
    }

    @Override
    public Set<Long> getNonReceivableFriendIds(Long memberId) {
        return friendshipQueryUseCase.getMutedIds(memberId);
    }
}