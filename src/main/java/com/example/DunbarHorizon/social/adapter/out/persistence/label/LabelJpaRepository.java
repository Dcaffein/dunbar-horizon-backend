package com.example.DunbarHorizon.social.adapter.out.persistence.label;

import com.example.DunbarHorizon.social.domain.label.Label;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LabelJpaRepository extends JpaRepository<Label, String> {

    @Query("""
            SELECT l
            FROM Label l
            WHERE l.id = :labelId
              AND EXISTS (SELECT u.id FROM ActiveUser u WHERE u.id = l.ownerId)
            """)
    Optional<Label> findByIdWithActiveOwner(@Param("labelId") String labelId);

    @Query("""
            SELECT l.labelName
            FROM ActiveUser u
            LEFT JOIN Label l ON l.ownerId = u.id AND (:excludedLabelId IS NULL OR l.id <> :excludedLabelId)
            WHERE u.id = :ownerId
            """)
    List<String> findNamesOfActiveOwner(@Param("ownerId") Long ownerId, @Param("excludedLabelId") String excludedLabelId);

}
