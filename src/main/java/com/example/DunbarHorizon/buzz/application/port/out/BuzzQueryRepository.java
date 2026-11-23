package com.example.DunbarHorizon.buzz.application.port.out;

import com.example.DunbarHorizon.buzz.application.dto.info.BuzzAudienceInfo;
import com.example.DunbarHorizon.buzz.application.dto.info.BuzzDetailInfo;
import com.example.DunbarHorizon.buzz.application.dto.info.BuzzSummaryInfo;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface BuzzQueryRepository {
    Optional<BuzzDetailInfo> findDetail(Long buzzId, LocalDateTime now);
    List<BuzzAudienceInfo> findAudience(Long buzzId);
    Slice<BuzzSummaryInfo> findReceived(Long userId, Set<Long> excludedCreatorIds, LocalDateTime now, Pageable pageable);
    Slice<BuzzSummaryInfo> findSent(Long creatorId, LocalDateTime now, Pageable pageable);
    List<Long> findUnreadCreatorIds(Long userId, Set<Long> excludedCreatorIds, LocalDateTime now);
}
