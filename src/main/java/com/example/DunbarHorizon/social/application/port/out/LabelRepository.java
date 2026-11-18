package com.example.DunbarHorizon.social.application.port.out;

import com.example.DunbarHorizon.social.domain.label.Label;

import java.util.Optional;

public interface LabelRepository {
    void create(Label label);
    void saveChanges(Label label);
    void delete(Label label);
    Optional<Label> findById(String labelId);
    Optional<Long> findOwnerId(String labelId);
    Optional<Long> lockOwnerId(String labelId);
    boolean existsByOwnerAndName(Long ownerId, String name);
    boolean existsByOwnerAndNameExcludingLabel(Long ownerId, String name, String labelId);
}
