package com.example.GooRoomBe.flag.adapter.out.persistence;

import com.example.GooRoomBe.flag.adapter.out.persistence.jpa.FlagMemorialJpaRepository;
import com.example.GooRoomBe.flag.domain.memorial.DeletableFlagMemorial;
import com.example.GooRoomBe.flag.domain.memorial.FlagMemorial;
import com.example.GooRoomBe.flag.domain.memorial.repository.FlagMemorialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class FlagMemorialRepositoryAdapter implements FlagMemorialRepository {

    private final FlagMemorialJpaRepository jpaRepository;

    @Override
    public FlagMemorial save(FlagMemorial memorial) {
        return jpaRepository.save(memorial);
    }

    @Override
    public Optional<FlagMemorial> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public boolean existsByFlagId(Long flagId) {
        return jpaRepository.existsByFlagId(flagId);
    }

    @Override
    public void delete(DeletableFlagMemorial memorial) {
        jpaRepository.delete(memorial.getEntity());
    }

    @Override
    public List<FlagMemorial> findAllMemorialsIfMemorialized(Long flagId, Long viewerId) {
        return jpaRepository.findAllMemorialsIfMemorialized(flagId, viewerId);
    }
}