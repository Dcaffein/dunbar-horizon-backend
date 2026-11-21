package com.example.DunbarHorizon.buzz.application.port.out;

import com.example.DunbarHorizon.buzz.application.dto.info.ReceivedBuzz;
import com.example.DunbarHorizon.buzz.domain.Buzz;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public interface BuzzQueryRepository {
    Slice<ReceivedBuzz> findUnexpiredReceived(Long userId, Set<Long> excludedCreatorIds, LocalDateTime now, Pageable pageable);
    Slice<Buzz> findUnexpiredSent(Long creatorId, LocalDateTime now, Pageable pageable);
    List<Long> findUnexpiredUnreadCreatorIds(Long userId, Set<Long> excludedCreatorIds, LocalDateTime now);
}
