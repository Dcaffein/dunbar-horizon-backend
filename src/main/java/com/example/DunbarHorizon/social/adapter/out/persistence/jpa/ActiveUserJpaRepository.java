package com.example.DunbarHorizon.social.adapter.out.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ActiveUserJpaRepository extends JpaRepository<ActiveUser, Long> {

    long countByIdIn(Collection<Long> ids);

    default boolean areActive(Long first, Long second) {
        return !first.equals(second) && countByIdIn(List.of(first, second)) == 2;
    }
}
