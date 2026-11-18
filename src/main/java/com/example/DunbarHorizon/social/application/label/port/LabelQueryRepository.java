package com.example.DunbarHorizon.social.application.label.port;

import com.example.DunbarHorizon.social.application.label.dto.LabelMemberResult;
import com.example.DunbarHorizon.social.application.label.dto.LabelResult;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface LabelQueryRepository {
    List<LabelResult> findAllByOwner(Long ownerId);
    List<LabelResult> findByOwnerAndMember(Long ownerId, Long memberId);
    Optional<LabelResult> findById(String labelId);
    Optional<Long> findOwnerId(String labelId);
    List<LabelMemberResult> findMembers(String labelId);
    Set<Long> findMemberIdsInOwnedLabels(Long ownerId, List<String> labelIds);
}
