package com.example.GooRoomBe.flag.adapter.out.client;

import com.example.GooRoomBe.account.application.port.in.UserQueryUseCase;
import com.example.GooRoomBe.account.application.port.in.dto.UserProfileDto;
import com.example.GooRoomBe.flag.application.port.out.FlagUserInfo;
import com.example.GooRoomBe.flag.application.port.out.FlagUserPort;
import com.example.GooRoomBe.flag.domain.flag.FriendshipChecker;
import com.example.GooRoomBe.social.application.port.in.FriendshipQueryUseCase;
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
        List<UserProfileDto> profiles = userQueryUseCase.getUserProfiles(writerIds);

        return profiles.stream()
                .collect(Collectors.toMap(
                        UserProfileDto::id,
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