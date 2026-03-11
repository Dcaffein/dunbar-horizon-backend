package com.example.DunbarHorizon.buzz.adapter.out.persistence.mongo;

import com.example.DunbarHorizon.buzz.domain.Buzz;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Set;

public interface BuzzSDMRepository extends MongoRepository<Buzz, String> {
    Slice<Buzz> findAllByRecipientIdsContainsAndCreatorIdNotInOrderByCreatedAtDesc(
            Long userId, Set<Long> blockedIds, Pageable pageable);
}