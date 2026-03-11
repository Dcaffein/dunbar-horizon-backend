package com.example.DunbarHorizon.buzz.application.port.in;

import com.example.DunbarHorizon.buzz.application.dto.result.BuzzDetailResult;
import com.example.DunbarHorizon.buzz.application.dto.result.BuzzSummaryResult;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.List;

public interface BuzzQueryUseCase {
    Slice<BuzzSummaryResult> getReceivedBuzzes(Long userId, Pageable pageable);
    BuzzDetailResult getBuzzDetail(Long userId, String buzzId);
    List<Long> getUnreadSenderIds(Long userId);
}
