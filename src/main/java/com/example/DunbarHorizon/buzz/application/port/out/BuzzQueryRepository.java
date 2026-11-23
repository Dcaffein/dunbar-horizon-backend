package com.example.DunbarHorizon.buzz.application.port.out;

import com.example.DunbarHorizon.buzz.application.dto.view.BuzzAudienceView;
import com.example.DunbarHorizon.buzz.application.dto.view.BuzzCommentView;
import com.example.DunbarHorizon.buzz.application.dto.view.BuzzDetailView;
import com.example.DunbarHorizon.buzz.application.dto.view.BuzzSummaryView;
import com.example.DunbarHorizon.buzz.application.dto.view.BuzzThreadDetailView;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface BuzzQueryRepository {
    Optional<BuzzDetailView> findDetail(Long buzzId);
    List<BuzzAudienceView> findAudience(Long buzzId);
    Optional<BuzzThreadDetailView> findThreadDetail(Long buzzId, Long threadId);
    List<BuzzCommentView> findComments(Long threadId);
    Slice<BuzzSummaryView> findReceived(Long userId, Set<Long> excludedCreatorIds, Pageable pageable);
    Slice<BuzzSummaryView> findSent(Long creatorId, Pageable pageable);
    List<Long> findUnreadCreatorIds(Long userId, Set<Long> excludedCreatorIds);
}
