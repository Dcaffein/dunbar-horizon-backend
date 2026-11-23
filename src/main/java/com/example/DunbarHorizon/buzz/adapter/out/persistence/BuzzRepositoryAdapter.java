package com.example.DunbarHorizon.buzz.adapter.out.persistence;

import com.example.DunbarHorizon.buzz.adapter.out.persistence.jpa.BuzzJpaRepository;
import com.example.DunbarHorizon.buzz.domain.Buzz;
import com.example.DunbarHorizon.buzz.application.port.out.BuzzRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class BuzzRepositoryAdapter implements BuzzRepository {

    private static final String PURGEABLE_IDS = """
            SELECT id FROM buzzes WHERE created_at <= :createdAtOrBefore
            UNION
            SELECT id FROM buzzes WHERE deleted_at IS NOT NULL
            """;

    private static final String DELETE_BY_IDS = "DELETE FROM buzzes WHERE id IN (:buzzIds)";

    private final BuzzJpaRepository buzzJpaRepository;
    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public Buzz save(Buzz buzz) {
        return buzzJpaRepository.save(buzz);
    }

    @Override
    public Optional<Buzz> findById(Long buzzId) {
        return buzzJpaRepository.findByIdAndDeletedAtIsNull(buzzId);
    }

    @Override
    public Optional<Buzz> findByIdForShare(Long buzzId) {
        return buzzJpaRepository.findByIdAndDeletedAtIsNullForShare(buzzId);
    }

    @Override
    public Optional<Buzz> findByIdForUpdate(Long buzzId) {
        return buzzJpaRepository.findByIdAndDeletedAtIsNullForUpdate(buzzId);
    }

    @Override
    public List<Long> findPurgeableIds(LocalDateTime createdAtOrBefore) {
        return jdbc.queryForList(PURGEABLE_IDS, Map.of("createdAtOrBefore", createdAtOrBefore), Long.class);
    }

    @Override
    public void deleteAllByIds(Collection<Long> buzzIds) {
        if (buzzIds.isEmpty()) {
            return;
        }
        jdbc.update(DELETE_BY_IDS, Map.of("buzzIds", buzzIds));
    }
}
