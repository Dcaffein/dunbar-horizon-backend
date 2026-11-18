package com.example.DunbarHorizon.social.adapter.out.persistence;

import com.example.DunbarHorizon.social.adapter.out.persistence.jpa.LabelJpaRepository;
import com.example.DunbarHorizon.social.adapter.out.persistence.jpa.ActiveUserJpaRepository;
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

import java.util.Optional;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class LabelPersistenceAdapter implements LabelRepository {

    private final LabelJpaRepository labelJpaRepository;
    private final ActiveUserJpaRepository activeUserJpaRepository;
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
        return labelJpaRepository.findById(labelId)
                .filter(label -> activeUserJpaRepository.existsById(label.getOwnerId()));
    }

    @Override
    public Optional<Long> findOwnerId(String labelId) {
        return findById(labelId).map(Label::getOwnerId);
    }

    @Override
    public Set<String> findNamesByOwner(Long ownerId, String excludedLabelId) {
        return labelJpaRepository.findNamesByOwner(ownerId, excludedLabelId);
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
