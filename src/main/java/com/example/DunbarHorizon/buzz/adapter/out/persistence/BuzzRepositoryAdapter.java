package com.example.DunbarHorizon.buzz.adapter.out.persistence;

import com.example.DunbarHorizon.buzz.adapter.out.persistence.mongo.BuzzMongoTemplateRepository;
import com.example.DunbarHorizon.buzz.adapter.out.persistence.mongo.BuzzSDMRepository;
import com.example.DunbarHorizon.buzz.domain.repository.BuzzRepository;
import com.example.DunbarHorizon.buzz.domain.Buzz;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class BuzzRepositoryAdapter implements BuzzRepository {

    private final BuzzSDMRepository buzzSDMRepository;
    private final BuzzMongoTemplateRepository buzzTemplateRepository;

    @Override
    public Buzz save(Buzz buzz) {
        return buzzSDMRepository.save(buzz);
    }

    @Override
    public Buzz findById(String id) {
        return buzzSDMRepository.findById(id).orElse(null);
    }

    @Override
    public void deleteById(String buzzId) {
        buzzSDMRepository.deleteById(buzzId);
    }

    @Override
    public Slice<Buzz> findAllByRecipientId(Long userId, Set<Long> excludedIds, Pageable pageable) {
        Set<Long> filterIds = (excludedIds == null || excludedIds.isEmpty()) ? Set.of(-1L) : excludedIds;
        return buzzSDMRepository.findAllByRecipientIdsContainsAndCreatorIdNotInOrderByCreatedAtDesc(
                userId, filterIds, pageable);
    }

    @Override
    public Slice<Buzz> findAllByCreatorId(Long userId, Pageable pageable) {
        return buzzSDMRepository.findAllByCreatorIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Override
    public List<Long> findUnreadSenderIds(Long userId, Set<Long> excludedIds) {
        Set<Long> filterIds = (excludedIds == null || excludedIds.isEmpty()) ? Set.of(-1L) : excludedIds;
        return buzzTemplateRepository.findUnreadSenderIds(userId, filterIds);
    }

    @Override
    public void addReadRecipient(String buzzId, Long userId) {
        buzzTemplateRepository.addReadRecipient(buzzId, userId);
    }
}
