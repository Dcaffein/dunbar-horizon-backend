package com.example.DunbarHorizon.social.application.port.out;

import com.example.DunbarHorizon.social.application.dto.result.LabelMemberResult;
import com.example.DunbarHorizon.social.application.dto.result.LabelResult;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface LabelQueryRepository {
    List<LabelResult> findAllByOwner(Long ownerId);
    List<LabelResult> findByOwnerAndMember(Long ownerId, Long memberId);
    Optional<LabelResult> findResultById(String labelId);
    List<LabelMemberResult> findMembers(String labelId);
    Set<Long> findMemberIdsByOwnerAndLabelIds(Long ownerId, List<String> labelIds);
}
