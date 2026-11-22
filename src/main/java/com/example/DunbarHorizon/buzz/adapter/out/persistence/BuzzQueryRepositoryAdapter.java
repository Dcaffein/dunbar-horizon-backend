package com.example.DunbarHorizon.buzz.adapter.out.persistence;

import com.example.DunbarHorizon.buzz.adapter.out.persistence.jpa.BuzzAudienceMemberJpaRepository;
import com.example.DunbarHorizon.buzz.adapter.out.persistence.jpa.BuzzDetailRow;
import com.example.DunbarHorizon.buzz.adapter.out.persistence.jpa.BuzzJpaRepository;
import com.example.DunbarHorizon.buzz.application.dto.info.BuzzAudienceInfo;
import com.example.DunbarHorizon.buzz.application.dto.info.BuzzDetailInfo;
import com.example.DunbarHorizon.buzz.application.dto.info.BuzzThreadInfo;
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
import java.util.Optional;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class BuzzQueryRepositoryAdapter implements BuzzQueryRepository {

    private static final Set<Long> NO_EXCLUDED_IDS = Set.of(-1L);

    private final BuzzJpaRepository buzzJpaRepository;
    private final BuzzAudienceMemberJpaRepository buzzAudienceMemberJpaRepository;

    @Override
    public Optional<BuzzDetailInfo> findDetail(Long buzzId, LocalDateTime now) {
        List<BuzzDetailRow> rows = buzzJpaRepository.findWithThreadsByIdAndExpiresAtAfterAndDeletedAtIsNull(buzzId, now);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        BuzzDetailRow buzz = rows.get(0);
        List<BuzzThreadInfo> threads = rows.stream()
                .filter(row -> row.threadId() != null)
                .map(row -> new BuzzThreadInfo(row.threadId(), row.starterId(), row.visibility(), row.threadCreatedAt()))
                .toList();
        return Optional.of(new BuzzDetailInfo(buzz.buzzId(), buzz.creatorId(), buzz.text(), buzz.imageKeys(),
                buzz.editedAt(), buzz.expiresAt(), threads));
    }

    @Override
    public List<BuzzAudienceInfo> findAudience(Long buzzId) {
        return buzzAudienceMemberJpaRepository.findAllByBuzzId(buzzId);
    }

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
