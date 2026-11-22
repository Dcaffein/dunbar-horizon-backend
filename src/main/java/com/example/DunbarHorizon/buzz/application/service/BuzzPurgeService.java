package com.example.DunbarHorizon.buzz.application.service;

import com.example.DunbarHorizon.buzz.domain.repository.BuzzAudienceMemberRepository;
import com.example.DunbarHorizon.buzz.domain.repository.BuzzCommentRepository;
import com.example.DunbarHorizon.buzz.domain.repository.BuzzRepository;
import com.example.DunbarHorizon.buzz.domain.repository.BuzzThreadRepository;
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
        List<Long> buzzIds = buzzRepository.findPurgeableIds(now);
        if (buzzIds.isEmpty()) {
            return 0;
        }
        buzzRepository.deleteAllByIds(buzzIds);
        List<Long> threadIds = buzzThreadRepository.findIdsByBuzzIds(buzzIds);
        buzzAudienceMemberRepository.deleteAllByBuzzIds(buzzIds);
        if (!threadIds.isEmpty()) {
            buzzCommentRepository.deleteAllByThreadIds(threadIds);
        }
        buzzThreadRepository.deleteAllByBuzzIds(buzzIds);
        return buzzIds.size();
    }
}
