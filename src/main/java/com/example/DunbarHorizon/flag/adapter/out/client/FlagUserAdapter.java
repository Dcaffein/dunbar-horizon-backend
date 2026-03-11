package com.example.DunbarHorizon.flag.adapter.out.client;

import com.example.DunbarHorizon.account.application.dto.UserProfileInfo;
import com.example.DunbarHorizon.account.application.port.in.UserQueryUseCase;
import com.example.DunbarHorizon.flag.application.dto.info.FlagUserInfo;
import com.example.DunbarHorizon.flag.application.port.out.FlagUserPort;
import com.example.DunbarHorizon.flag.domain.flag.FriendshipChecker;
import com.example.DunbarHorizon.social.application.port.in.FriendshipQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class FlagUserAdapter implements FlagUserPort, FriendshipChecker {

    private final FriendshipQueryUseCase friendshipQueryUseCase;
    private final UserQueryUseCase userQueryUseCase;

    @Override
    public Set<Long> getRelatedUserIds(Long userId) {
        return friendshipQueryUseCase.getListenableFriendIds(userId);
    }

    @Override
    public Map<Long, FlagUserInfo> findUserInfosByIds(Collection<Long> writerIds) {
        List<UserProfileInfo> profiles = userQueryUseCase.getUserProfiles(writerIds);

        return profiles.stream()
                .collect(Collectors.toMap(
                        UserProfileInfo::id,
                        dto -> new FlagUserInfo(
                                dto.id(),
                                dto.nickname(),
                                dto.profileImage()
                        )
                ));
    }

    @Override
    public boolean areFriends(Long userId1, Long userId2) {
        return friendshipQueryUseCase.areFriends(userId1, userId2);
    }
}