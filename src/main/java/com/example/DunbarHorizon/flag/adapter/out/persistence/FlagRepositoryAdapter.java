package com.example.DunbarHorizon.flag.adapter.out.persistence;

import com.example.DunbarHorizon.flag.adapter.out.persistence.jpa.FlagJpaRepository;
import com.example.DunbarHorizon.flag.domain.flag.Flag;
import com.example.DunbarHorizon.flag.domain.flag.FlagStatus;
import com.example.DunbarHorizon.flag.domain.flag.repository.FlagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class FlagRepositoryAdapter implements FlagRepository {

    // JPA 인터페이스를 주입받아 사용 (의존성 숨김)
    private final FlagJpaRepository jpaRepository;

    @Override
    public Flag save(Flag flag) {
        return jpaRepository.save(flag);
    }

    @Override
    public Optional<Flag> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<Flag> findByIdExclusive(Long id) {
        return jpaRepository.findByIdExclusive(id);
    }

    @Override
    public Optional<Flag> findByParentId(Long parentId) {
        return jpaRepository.findByParentId(parentId);
    }

    @Override
    public boolean existsById(Long id) {
        return jpaRepository.existsById(id);
    }

    @Override
    public int expireAllExceedingThreshold(LocalDateTime threshold) {
        return jpaRepository.expireAllExceedingThreshold(threshold);
    }

    @Override
    public boolean existsByParentId(Long parentId) {
        return jpaRepository.existsByParentId(parentId);
    }

    @Override
    public List<Flag> findAllByIdIn(Collection<Long> ids) {
        return jpaRepository.findAllByIdIn(ids);
    }

    @Override
    public List<Flag> findAllByHostId(Long hostId) {
        return jpaRepository.findAllByHostId(hostId);
    }

    @Override
    public List<Flag> findAllByHostIdsAndStatus(Set<Long> friendIds, FlagStatus flagStatus) {
        if (friendIds == null || friendIds.isEmpty()) {
            return List.of();
        }

        LocalDateTime now = LocalDateTime.now();
        return switch (flagStatus) {
            case RECRUITING ->
                    jpaRepository.findRecruitingByHostIds(friendIds, now);
            case WAITING ->
                    jpaRepository.findBeforeActivityByHostIds(friendIds, now);
            case IN_ACTIVITY ->
                    jpaRepository.findInProgressByHostIds(friendIds, now);
            case ENDED ->
                    jpaRepository.findEndedByHostIds(friendIds, now);
            default ->
                    throw new IllegalArgumentException("조회를 지원하지 않는 플래그 상태입니다: " + flagStatus);
        };
    }
}