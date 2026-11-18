package com.example.DunbarHorizon.social.application.port.out;

import com.example.DunbarHorizon.social.domain.label.Label;

import java.util.Optional;
import java.util.Set;

public interface LabelRepository {
    void save(Label label);
    void create(Label label);
    void saveChanges(Label label);
    void delete(Label label);
    Optional<Label> findById(String labelId);
    Optional<Long> findOwnerId(String labelId);
    Set<String> findNamesByOwner(Long ownerId, String excludedLabelId);
}
