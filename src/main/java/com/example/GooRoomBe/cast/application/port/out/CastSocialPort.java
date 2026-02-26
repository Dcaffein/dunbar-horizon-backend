package com.example.GooRoomBe.cast.application.port.out;

import com.example.GooRoomBe.cast.application.port.out.dto.CastCreatorDto;

import java.util.List;
import java.util.Set;

public interface CastSocialPort {
    Set<Long> getMemberIdsByLabels(Long creatorId, List<String> labelIds);
    Set<Long> filterOnlyFriends(Long creatorId, List<Long> targetIds);
    Set<Long> getPivotRecipientIds(Long creatorId, Long pivotFriendId, Double expansionValue);
    List<CastCreatorDto> getCreatorProfiles(List<Long> userId);
    Set<Long> getNonReceivableFriendIds(Long memberId);
}