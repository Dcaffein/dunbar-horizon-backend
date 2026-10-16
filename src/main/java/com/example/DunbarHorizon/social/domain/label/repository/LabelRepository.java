package com.example.DunbarHorizon.social.domain.label.repository;

import com.example.DunbarHorizon.social.domain.label.Label;

import java.util.Collection;
import java.util.Set;
import java.util.List;
import java.util.Optional;

public interface LabelRepository {
    void create(Label label);
    void rename(String labelId, String labelName);
    void delete(String labelId);
    boolean existsMember(String labelId, Long memberId);
    void addMember(String labelId, Long memberId);
    void removeMember(String labelId, Long memberId);
    void replaceMembers(String labelId, Collection<Long> memberIds);
    void removeMemberFromOwnedLabels(Long ownerId, Long memberId);

    boolean existsByOwner_IdAndLabelName(Long ownerId, String labelName);
    List<Label> findAllByOwner_Id(Long ownerId);
    Optional<Label> findById(String labelId);
    List<Label> findLabelsByOwnerAndMember(Long ownerId, Long memberId);
    Set<Long> findMemberIdsByOwnerAndLabelIds(Long ownerId, List<String> labelIds);
}
