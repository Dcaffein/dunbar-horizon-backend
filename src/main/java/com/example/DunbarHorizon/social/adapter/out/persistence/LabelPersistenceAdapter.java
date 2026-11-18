package com.example.DunbarHorizon.social.adapter.out.persistence;

import com.example.DunbarHorizon.social.adapter.out.persistence.jpa.LabelJpaRepository;
import com.example.DunbarHorizon.social.application.port.out.LabelRepository;
import com.example.DunbarHorizon.social.domain.label.Label;
import com.example.DunbarHorizon.social.domain.label.exception.DuplicateLabelMemberException;
import com.example.DunbarHorizon.social.domain.label.exception.DuplicateLabelNameException;
import com.example.DunbarHorizon.social.domain.label.exception.LabelConflictException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class LabelPersistenceAdapter implements LabelRepository {

    private final LabelJpaRepository labelJpaRepository;
    private final EntityManager entityManager;

    @Override
    public void save(Label label) {
        labelJpaRepository.save(label);
        flush(label);
    }

    @Override
    public void delete(Label label) {
        entityManager.remove(label);
        flush(label);
    }

    @Override
    public Optional<Label> findById(String labelId) {
        return labelJpaRepository.findByIdWithActiveOwner(labelId);
    }

    @Override
    public Optional<Set<String>> findNamesOfActiveOwner(Long ownerId, String excludedLabelId) {
        var names = labelJpaRepository.findNamesOfActiveOwner(ownerId, excludedLabelId);
        if (names.isEmpty()) {
            return Optional.empty();
        }
        // An active owner without other labels has one row with a null name.
        return Optional.of(names.stream().filter(Objects::nonNull).collect(Collectors.toUnmodifiableSet()));
    }

    private void flush(Label label) {
        try {
            entityManager.flush();
        } catch (OptimisticLockException exception) {
            throw new LabelConflictException(label.getId());
        } catch (PersistenceException exception) {
            if (PersistenceErrors.violates(exception, "uq_labels_owner_name")) {
                throw new DuplicateLabelNameException(label.getOwnerId(), label.getLabelName());
            }
            if (PersistenceErrors.violatesPrimaryKey(exception, "label_members")) {
                throw new DuplicateLabelMemberException(label.getId());
            }
            throw exception;
        }
    }
}
