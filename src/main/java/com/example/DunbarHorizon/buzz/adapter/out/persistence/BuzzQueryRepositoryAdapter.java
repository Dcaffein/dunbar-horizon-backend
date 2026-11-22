package com.example.DunbarHorizon.buzz.adapter.out.persistence;

import com.example.DunbarHorizon.buzz.adapter.out.persistence.jpa.BuzzJpaRepository;
import com.example.DunbarHorizon.buzz.application.dto.info.ReceivedBuzz;
import com.example.DunbarHorizon.buzz.application.port.out.BuzzQueryRepository;
import com.example.DunbarHorizon.buzz.domain.Buzz;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class BuzzQueryRepositoryAdapter implements BuzzQueryRepository {

    private static final Set<Long> NO_EXCLUDED_IDS = Set.of(-1L);

    private final BuzzJpaRepository buzzJpaRepository;

    @Override
    public Slice<ReceivedBuzz> findReceived(Long userId, Set<Long> excludedCreatorIds,
                                                     LocalDateTime now, Pageable pageable) {
        return buzzJpaRepository.findAllWithReadByMemberUserIdAndExpiresAtAfterAndDeletedAtIsNullAndCreatorIdNotIn(
                userId, now, nonEmpty(excludedCreatorIds), unsorted(pageable));
    }

    @Override
    public Slice<Buzz> findSent(Long creatorId, LocalDateTime now, Pageable pageable) {
        return buzzJpaRepository.findAllByCreatorIdAndExpiresAtAfterAndDeletedAtIsNull(creatorId, now, unsorted(pageable));
    }

    @Override
    public List<Long> findUnreadCreatorIds(Long userId, Set<Long> excludedCreatorIds, LocalDateTime now) {
        return buzzJpaRepository.findCreatorIdsByMemberUserIdAndMemberIsReadFalseAndExpiresAtAfterAndDeletedAtIsNullAndCreatorIdNotIn(
                userId, now, nonEmpty(excludedCreatorIds));
    }

    private static Set<Long> nonEmpty(Set<Long> ids) {
        return ids == null || ids.isEmpty() ? NO_EXCLUDED_IDS : ids;
    }

    private static Pageable unsorted(Pageable pageable) {
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
    }
}
