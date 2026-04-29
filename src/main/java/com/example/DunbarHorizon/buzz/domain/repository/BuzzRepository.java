package com.example.DunbarHorizon.buzz.domain.repository;

import com.example.DunbarHorizon.buzz.domain.Buzz;
import com.example.DunbarHorizon.buzz.domain.BuzzComment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.List;
import java.util.Set;

public interface BuzzRepository {
    Buzz save(Buzz buzz);
    Buzz findById(String id);
    void addComment(String buzzId, BuzzComment comment);
    void updateComment(String buzzId, String commentId, String text, List<String> imageUrls);
    void removeComment(String buzzId, String commentId);
    void deleteById(String buzzId);
    Slice<Buzz> findAllByRecipientId(Long userId, Set<Long> excludedIds, Pageable pageable);
    List<Long> findUnreadSenderIds(Long userId, Set<Long> excludedIds);
    void addReadRecipient(String buzzId, Long userId);
}
