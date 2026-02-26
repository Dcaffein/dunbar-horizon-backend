package com.example.GooRoomBe.flag.domain.memorial.repository;

import com.example.GooRoomBe.flag.domain.memorial.DeletableFlagMemorial;
import com.example.GooRoomBe.flag.domain.memorial.FlagMemorial;

import java.util.List;
import java.util.Optional;

public interface FlagMemorialRepository {
    FlagMemorial save(FlagMemorial memorial);
    Optional<FlagMemorial> findById(Long id);
    boolean existsByFlagId(Long flagId);
    void delete(DeletableFlagMemorial memorial);
    List<FlagMemorial> findAllMemorialsIfMemorialized(Long flagId, Long viewerId);
}