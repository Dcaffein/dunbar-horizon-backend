package com.example.DunbarHorizon.buzz.domain.repository;

import com.example.DunbarHorizon.buzz.domain.Buzz;
import com.example.DunbarHorizon.buzz.domain.BuzzReply;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.List;
import java.util.Set;

public interface BuzzRepository {
    Buzz save(Buzz buzz);
    Buzz findById(String id);
    void addReply(String buzzId, BuzzReply reply);
    void updateReply(String buzzId, String replyId, String text, List<String> strings);
    void removeReply(String buzzId, String replyId);
    void deleteById(String buzzId);
    Slice<Buzz> findAllByRecipientId(Long userId, Set<Long> excludedIds, Pageable pageable);
    List<Long> findUnreadSenderIds(Long userId, Set<Long> excludedIds);
    void addReadRecipient(String buzzId, Long userId);
}