package com.example.DunbarHorizon.social.application.label.port;

import com.example.DunbarHorizon.social.domain.label.Label;

import java.util.Optional;
import java.util.Set;

public interface LabelRepository {
    void save(Label label);
    void delete(Label label);
    Optional<Label> findById(String labelId);

    /**
     * Returns empty for a missing or inactive owner, and an empty set for an active owner with no matching labels.
     */
    Optional<Set<String>> findNamesOfActiveOwner(Long ownerId, String excludedLabelId);
}
