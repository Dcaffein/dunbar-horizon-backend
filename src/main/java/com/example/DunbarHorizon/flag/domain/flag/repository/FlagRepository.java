package com.example.DunbarHorizon.flag.domain.flag.repository;

import com.example.DunbarHorizon.flag.domain.flag.Flag;
import com.example.DunbarHorizon.flag.domain.flag.FlagStatus;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface FlagRepository {
    Flag save(Flag flag);
    Optional<Flag> findById(Long id);
    Optional<Flag> findByIdExclusive(Long id);
    Optional<Flag> findByParentId(Long parentId);
    boolean existsById(Long id);
    int expireAllExceedingThreshold(LocalDateTime threshold);
    boolean existsByParentId(Long parentId);
    List<Flag> findAllByIdIn(Collection<Long> ids);
    List<Flag> findAllByHostId(Long hostId);
    List<Flag> findAllByHostIdsAndStatus(Set<Long> friendIds, FlagStatus flagStatus);
}