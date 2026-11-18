package com.example.DunbarHorizon.social.adapter.out.persistence.jpa;

import com.example.DunbarHorizon.social.domain.label.Label;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface LabelJpaRepository extends JpaRepository<Label, String> {

    boolean existsByOwnerIdAndLabelName(Long ownerId, String labelName);

    boolean existsByOwnerIdAndLabelNameAndIdNot(Long ownerId, String labelName, String id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Label> findLockedById(String id);
}
