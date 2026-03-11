package com.example.DunbarHorizon.buzz.application.port.out;

import com.example.DunbarHorizon.buzz.application.dto.info.BuzzCreatorInfo;

import java.util.List;
import java.util.Set;

public interface BuzzSocialPort {
    List<BuzzCreatorInfo> getCreatorProfiles(List<Long> userIds);
    Set<Long> getNonReceivableFriendIds(Long memberId);
}
