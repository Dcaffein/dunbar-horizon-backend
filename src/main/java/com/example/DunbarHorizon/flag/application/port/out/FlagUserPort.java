package com.example.DunbarHorizon.flag.application.port.out;

import com.example.DunbarHorizon.flag.application.dto.info.FlagUserInfo;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface FlagUserPort {
    Set<Long> getRelatedUserIds(Long userId);
    Map<Long, FlagUserInfo> findUserInfosByIds(Collection<Long> userIds);
}