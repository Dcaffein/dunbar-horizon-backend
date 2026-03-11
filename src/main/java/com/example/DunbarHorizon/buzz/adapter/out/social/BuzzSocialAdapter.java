package com.example.DunbarHorizon.buzz.adapter.out.social;

import com.example.DunbarHorizon.account.application.port.in.UserQueryUseCase;
import com.example.DunbarHorizon.buzz.application.dto.info.BuzzCreatorInfo;
import com.example.DunbarHorizon.buzz.application.port.out.BuzzSocialPort;
import com.example.DunbarHorizon.social.application.port.in.FriendshipQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class BuzzSocialAdapter implements BuzzSocialPort {

    private final UserQueryUseCase userQueryUseCase;
    private final FriendshipQueryUseCase friendshipQueryUseCase;

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
}
