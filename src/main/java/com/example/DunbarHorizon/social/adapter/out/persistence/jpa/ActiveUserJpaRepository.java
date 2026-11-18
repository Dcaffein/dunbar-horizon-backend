package com.example.DunbarHorizon.social.adapter.out.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface ActiveUserJpaRepository extends JpaRepository<ActiveUser, Long> {

    long countByIdIn(Collection<Long> ids);

    @Query("SELECT u.id FROM ActiveUser u WHERE u.id IN :ids")
    Set<Long> findActiveIds(@Param("ids") Collection<Long> ids);

    default boolean areActive(Long first, Long second) {
        return !first.equals(second) && countByIdIn(List.of(first, second)) == 2;
    }
}
