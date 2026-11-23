package com.example.DunbarHorizon.buzz.domain.repository;

import com.example.DunbarHorizon.buzz.domain.BuzzAudienceMember;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BuzzAudienceMemberRepository {
    BuzzAudienceMember save(BuzzAudienceMember buzzAudienceMember);
    List<BuzzAudienceMember> saveAll(List<BuzzAudienceMember> buzzAudienceMembers);
    Optional<BuzzAudienceMember> findByBuzzIdAndUserId(Long buzzId, Long userId);
    void deleteAllByBuzzIds(Collection<Long> buzzIds);
}
