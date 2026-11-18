package com.example.DunbarHorizon.social.adapter.out.persistence.jpa;

import com.example.DunbarHorizon.social.domain.label.Label;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LabelJpaRepository extends JpaRepository<Label, String> {

    boolean existsByOwnerIdAndLabelName(Long ownerId, String labelName);

    boolean existsByOwnerIdAndLabelNameAndIdNot(Long ownerId, String labelName, String id);

}
