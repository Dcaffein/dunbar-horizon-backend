package com.example.DunbarHorizon.buzz.domain.repository;

import com.example.DunbarHorizon.buzz.domain.BuzzAudienceMember;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BuzzAudienceMemberRepository {
    List<BuzzAudienceMember> saveAll(List<BuzzAudienceMember> buzzAudienceMembers);
    Optional<BuzzAudienceMember> findByBuzzIdAndUserId(String buzzId, Long userId);
    List<Long> findUserIdsByBuzzId(String buzzId);
    void deleteAllByBuzzId(String buzzId);
    void deleteAllByBuzzIds(Collection<String> buzzIds);
}
