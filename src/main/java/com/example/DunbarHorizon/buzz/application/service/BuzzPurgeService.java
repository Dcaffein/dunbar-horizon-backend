package com.example.DunbarHorizon.buzz.application.service;

import com.example.DunbarHorizon.buzz.application.port.out.BuzzPurgeRepository;
import com.example.DunbarHorizon.buzz.domain.Buzz;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BuzzPurgeService {

    private final BuzzPurgeRepository buzzPurgeRepository;

    public int purgeExpiredAndDeletedBuzzes(LocalDateTime now) {
        List<Long> buzzIds = buzzPurgeRepository.findPurgeableIds(now.minus(Buzz.LIFETIME));
        if (buzzIds.isEmpty()) {
            return 0;
        }
        buzzPurgeRepository.purge(buzzIds);
        return buzzIds.size();
    }
}
