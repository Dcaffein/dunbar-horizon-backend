package com.example.GooRoomBe.cast.adapter.out.persistence.adapter;

import com.example.GooRoomBe.cast.adapter.out.persistence.mongo.CastMongoTemplateRepository;
import com.example.GooRoomBe.cast.adapter.out.persistence.mongo.CastSDMRepository;
import com.example.GooRoomBe.cast.domain.model.Cast;
import com.example.GooRoomBe.cast.domain.model.CastReply;
import com.example.GooRoomBe.cast.domain.repository.CastRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class CastRepositoryAdapter implements CastRepository {

    private final CastSDMRepository castSDMRepository;
    private final CastMongoTemplateRepository castTemplateRepository;

    @Override
    public Cast save(Cast cast) {
        return castSDMRepository.save(cast);
    }

    @Override
    public Cast findById(String id) {
        return castSDMRepository.findById(id).orElse(null);
    }

    @Override
    public void addReply(String castId, CastReply reply) {
        castTemplateRepository.addReply(castId, reply);
    }

    @Override
    public void updateReply(String castId, String replyId, String text, List<String> imageUrls) {
        castTemplateRepository.updateReply(castId, replyId, text, imageUrls);
    }

    @Override
    public void removeReply(String castId, String replyId) {
        castTemplateRepository.removeReply(castId, replyId);
    }

    @Override
    public void deleteById(String castId) {
        castSDMRepository.deleteById(castId);
    }

    @Override
    public Slice<Cast> findAllByRecipientId(Long userId, Set<Long> excludedIds, Pageable pageable) {
        // 차단 리스트가 비어있을 경우 nin 쿼리 오류 방지를 위한 처리
        Set<Long> filterIds = (excludedIds == null || excludedIds.isEmpty()) ? Set.of(-1L) : excludedIds;
        return castSDMRepository.findAllByRecipientIdsContainsAndCreatorIdNotInOrderByCreatedAtDesc(
                userId, filterIds, pageable);
    }

    @Override
    public List<Long> findUnreadSenderIds(Long userId, Set<Long> excludedIds) {
        Set<Long> filterIds = (excludedIds == null || excludedIds.isEmpty()) ? Set.of(-1L) : excludedIds;
        return castTemplateRepository.findUnreadSenderIds(userId, filterIds);
    }
}