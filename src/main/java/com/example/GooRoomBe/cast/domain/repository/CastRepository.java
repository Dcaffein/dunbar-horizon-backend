package com.example.GooRoomBe.cast.domain.repository;

import com.example.GooRoomBe.cast.domain.model.Cast;
import com.example.GooRoomBe.cast.domain.model.CastReply;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.List;
import java.util.Set;

public interface CastRepository {
    Cast save(Cast cast);
    Cast findById(String id);
    void addReply(String castId, CastReply reply);
    void updateReply(String castId, String replyId, String text, List<String> strings);
    void removeReply(String castId, String replyId);
    void deleteById(String castId);
    Slice<Cast> findAllByRecipientId(Long userId, Set<Long> excludedIds, Pageable pageable);
    List<Long> findUnreadSenderIds(Long userId, Set<Long> excludedIds);
}