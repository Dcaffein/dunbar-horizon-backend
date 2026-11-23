package com.example.DunbarHorizon.buzz.application.port.out;

import com.example.DunbarHorizon.buzz.application.dto.info.BuzzAudienceInfo;
import com.example.DunbarHorizon.buzz.application.dto.info.BuzzCommentInfo;
import com.example.DunbarHorizon.buzz.application.dto.info.BuzzDetailInfo;
import com.example.DunbarHorizon.buzz.application.dto.info.BuzzSummaryInfo;
import com.example.DunbarHorizon.buzz.application.dto.info.BuzzThreadDetailInfo;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface BuzzQueryRepository {
    Optional<BuzzDetailInfo> findDetail(Long buzzId);
    List<BuzzAudienceInfo> findAudience(Long buzzId);
    Optional<BuzzThreadDetailInfo> findThreadDetail(Long buzzId, Long threadId);
    List<BuzzCommentInfo> findComments(Long threadId);
    Slice<BuzzSummaryInfo> findReceived(Long userId, Set<Long> excludedCreatorIds, Pageable pageable);
    Slice<BuzzSummaryInfo> findSent(Long creatorId, Pageable pageable);
    List<Long> findUnreadCreatorIds(Long userId, Set<Long> excludedCreatorIds);
}
