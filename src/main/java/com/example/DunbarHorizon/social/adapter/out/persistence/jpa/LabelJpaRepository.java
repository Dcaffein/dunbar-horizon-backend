package com.example.DunbarHorizon.social.adapter.out.persistence.jpa;

import com.example.DunbarHorizon.social.domain.label.Label;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Set;

public interface LabelJpaRepository extends JpaRepository<Label, String> {

    @Query("""
            select l.labelName from Label l
            where l.ownerId = :ownerId and (:excludedLabelId is null or l.id <> :excludedLabelId)
            """)
    Set<String> findNamesByOwner(@Param("ownerId") Long ownerId, @Param("excludedLabelId") String excludedLabelId);

}
