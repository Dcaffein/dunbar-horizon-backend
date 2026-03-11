package com.example.DunbarHorizon.social.domain.label.repository;

import com.example.DunbarHorizon.social.domain.label.Label;

import java.util.Set;
import java.util.List;
import java.util.Optional;

public interface LabelRepository {
    Label save(Label label);
    boolean existsByOwner_IdAndLabelName(Long ownerId, String labelName);
    List<Label> findAllByOwner_Id(Long ownerId);
    void delete(Label label);
    Optional<Label> findById(String labelId);
    void saveAll(List<Label> labels);
    Set<Long> findMemberIdsByOwnerAndLabelIds(Long ownerId, List<String> labelIds);
}
