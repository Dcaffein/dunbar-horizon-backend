package com.example.DunbarHorizon.buzz.adapter.out.persistence;

import com.example.DunbarHorizon.buzz.adapter.out.persistence.mongo.BuzzMongoTemplateRepository;
import com.example.DunbarHorizon.buzz.adapter.out.persistence.mongo.BuzzSDMRepository;
import com.example.DunbarHorizon.buzz.domain.repository.BuzzRepository;
import com.example.DunbarHorizon.buzz.domain.Buzz;
import com.example.DunbarHorizon.buzz.domain.BuzzReply;
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
        return buzzTemplateRepository.findByIdWithReplySlice(id);
    }

    @Override
    public void addReply(String buzzId, BuzzReply reply) {
        buzzTemplateRepository.addReply(buzzId, reply);
    }

    @Override
    public void updateReply(String buzzId, String replyId, String text, List<String> imageUrls) {
        buzzTemplateRepository.updateReply(buzzId, replyId, text, imageUrls);
    }

    @Override
    public void removeReply(String buzzId, String replyId) {
        buzzTemplateRepository.removeReply(buzzId, replyId);
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
    public List<Long> findUnreadSenderIds(Long userId, Set<Long> excludedIds) {
        Set<Long> filterIds = (excludedIds == null || excludedIds.isEmpty()) ? Set.of(-1L) : excludedIds;
        return buzzTemplateRepository.findUnreadSenderIds(userId, filterIds);
    }

    @Override
    public void addReadRecipient(String buzzId, Long userId) {
        buzzTemplateRepository.addReadRecipient(buzzId, userId);
    }
}
