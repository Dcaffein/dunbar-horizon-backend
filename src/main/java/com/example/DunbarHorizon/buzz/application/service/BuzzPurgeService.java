package com.example.DunbarHorizon.buzz.application.service;

import com.example.DunbarHorizon.buzz.domain.Buzz;
import com.example.DunbarHorizon.buzz.application.port.out.BuzzAudienceMemberRepository;
import com.example.DunbarHorizon.buzz.application.port.out.BuzzCommentRepository;
import com.example.DunbarHorizon.buzz.application.port.out.BuzzRepository;
import com.example.DunbarHorizon.buzz.application.port.out.BuzzThreadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BuzzPurgeService {

    private final BuzzRepository buzzRepository;
    private final BuzzAudienceMemberRepository buzzAudienceMemberRepository;
    private final BuzzThreadRepository buzzThreadRepository;
    private final BuzzCommentRepository buzzCommentRepository;

    public int purgeExpiredAndDeletedBuzzes(LocalDateTime now) {
        List<Long> buzzIds = buzzRepository.findPurgeableIds(now.minus(Buzz.LIFETIME));
        if (buzzIds.isEmpty()) {
            return 0;
        }
        buzzRepository.deleteAllByIds(buzzIds);
        List<Long> threadIds = buzzThreadRepository.findIdsByBuzzIds(buzzIds);
        buzzAudienceMemberRepository.deleteAllByBuzzIds(buzzIds);
        buzzThreadRepository.deleteAllByBuzzIds(buzzIds);
        if (!threadIds.isEmpty()) {
            buzzCommentRepository.deleteAllByThreadIds(threadIds);
        }
        return buzzIds.size();
    }
}
