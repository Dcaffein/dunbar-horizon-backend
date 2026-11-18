package com.example.DunbarHorizon.social.adapter.out.persistence;

import com.example.DunbarHorizon.social.adapter.out.persistence.jpa.LabelJpaRepository;
import com.example.DunbarHorizon.social.adapter.out.persistence.jpa.ActiveUserJpaRepository;
import com.example.DunbarHorizon.social.application.port.out.LabelRepository;
import com.example.DunbarHorizon.social.domain.label.Label;
import com.example.DunbarHorizon.social.domain.label.exception.DuplicateLabelNameException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class LabelPersistenceAdapter implements LabelRepository {

    private final LabelJpaRepository labelJpaRepository;
    private final ActiveUserJpaRepository activeUserJpaRepository;
    private final EntityManager entityManager;

    @Override
    public void create(Label label) {
        entityManager.persist(label);
        saveChanges(label);
    }

    @Override
    public void saveChanges(Label label) {
        try {
            entityManager.flush();
        } catch (PersistenceException exception) {
            if (PersistenceErrors.violates(exception, "uq_labels_owner_name")) {
                throw new DuplicateLabelNameException(label.getOwnerId(), label.getLabelName());
            }
            throw exception;
        }
    }

    @Override
    public void delete(Label label) {
        entityManager.remove(label);
        entityManager.flush();
    }

    @Override
    public Optional<Label> findById(String labelId) {
        return labelJpaRepository.findById(labelId)
                .filter(label -> activeUserJpaRepository.existsById(label.getOwnerId()));
    }

    @Override
    public Optional<Long> findOwnerId(String labelId) {
        return findById(labelId).map(Label::getOwnerId);
    }

    @Override
    public Optional<Long> lockOwnerId(String labelId) {
        return labelJpaRepository.findLockedById(labelId).map(Label::getOwnerId);
    }

    @Override
    public boolean existsByOwnerAndName(Long ownerId, String name) {
        return labelJpaRepository.existsByOwnerIdAndLabelName(ownerId, name);
    }

    @Override
    public boolean existsByOwnerAndNameExcludingLabel(Long ownerId, String name, String labelId) {
        return labelJpaRepository.existsByOwnerIdAndLabelNameAndIdNot(ownerId, name, labelId);
    }
}
