package com.example.DunbarHorizon.buzz.application.service;

import com.example.DunbarHorizon.buzz.application.port.out.BuzzPurgeRepository;
import com.example.DunbarHorizon.buzz.domain.Buzz;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BuzzPurgeService {

    static final int BATCH_SIZE = 1000;

    private final BuzzPurgeRepository buzzPurgeRepository;
    private final TransactionTemplate batchTransaction;

    public BuzzPurgeService(BuzzPurgeRepository buzzPurgeRepository, PlatformTransactionManager transactionManager) {
        this.buzzPurgeRepository = buzzPurgeRepository;
        this.batchTransaction = new TransactionTemplate(transactionManager);
    }

    public int purgeExpiredAndDeletedBuzzes(LocalDateTime now) {
        LocalDateTime createdAtOrBefore = now.minus(Buzz.LIFETIME);
        int total = 0;
        int purged;
        do {
            purged = batchTransaction.execute(status -> purgeBatch(createdAtOrBefore));
            total += purged;
        } while (purged == BATCH_SIZE);
        return total;
    }

    private int purgeBatch(LocalDateTime createdAtOrBefore) {
        List<Long> buzzIds = buzzPurgeRepository.findPurgeableIds(createdAtOrBefore, BATCH_SIZE);
        if (!buzzIds.isEmpty()) {
            buzzPurgeRepository.purge(buzzIds);
        }
        return buzzIds.size();
    }
}
