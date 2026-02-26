package com.example.GooRoomBe.cast.adapter.out.persistence.mongo;

import com.example.GooRoomBe.cast.domain.model.Cast;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Set;

public interface CastSDMRepository extends MongoRepository<Cast, String> {
    Slice<Cast> findAllByRecipientIdsContainsAndCreatorIdNotInOrderByCreatedAtDesc(
            Long userId, Set<Long> blockedIds, Pageable pageable);
}