package com.example.GooRoomBe.flag.application.port.out;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface FlagUserPort {
    Set<Long> getRelatedUserIds(Long userId);
    Map<Long, FlagUserInfo> findUserInfosByIds(Collection<Long> userIds);
    boolean areFriends(Long userId1, Long userId2);
}