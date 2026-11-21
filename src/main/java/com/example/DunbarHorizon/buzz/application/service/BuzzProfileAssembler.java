package com.example.DunbarHorizon.buzz.application.service;

import com.example.DunbarHorizon.buzz.application.dto.info.BuzzCreatorInfo;
import com.example.DunbarHorizon.buzz.application.dto.result.BuzzProfileResult;
import com.example.DunbarHorizon.buzz.application.port.out.BuzzSocialPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BuzzProfileAssembler {

    private final BuzzSocialPort buzzSocialPort;

    public Profiles profilesOf(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return new Profiles(Map.of());
        }
        Map<Long, BuzzProfileResult> profiles = buzzSocialPort
                .getCreatorProfiles(new ArrayList<>(new LinkedHashSet<>(userIds))).stream()
                .map(BuzzProfileAssembler::toProfileResult)
                .collect(Collectors.toMap(BuzzProfileResult::userId, Function.identity(), (first, second) -> first));
        return new Profiles(profiles);
    }

    private static BuzzProfileResult toProfileResult(BuzzCreatorInfo info) {
        return new BuzzProfileResult(info.id(), info.nickname(), info.profileImageUrl());
    }

    public record Profiles(Map<Long, BuzzProfileResult> byUserId) {

        public BuzzProfileResult of(Long userId) {
            return byUserId.getOrDefault(userId, new BuzzProfileResult(userId, null, null));
        }

        public List<BuzzProfileResult> foundOf(Collection<Long> userIds) {
            return userIds.stream().map(byUserId::get).filter(Objects::nonNull).toList();
        }
    }
}
