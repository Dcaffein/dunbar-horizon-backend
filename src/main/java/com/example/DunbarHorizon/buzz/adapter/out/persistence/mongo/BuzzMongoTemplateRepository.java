package com.example.DunbarHorizon.buzz.adapter.out.persistence.mongo;

import com.example.DunbarHorizon.buzz.domain.Buzz;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class BuzzMongoTemplateRepository {

    private final MongoTemplate mongoTemplate;

    public List<Long> findUnreadSenderIds(Long userId, Set<Long> mutedIds) {
        Criteria criteria = Criteria.where(BuzzField.RECIPIENT_IDS).is(userId)
                .and(BuzzField.READ_RECIPIENTS).ne(userId)
                .and(BuzzField.CREATOR_ID).nin(mutedIds);

        return mongoTemplate.findDistinct(new Query(criteria), BuzzField.CREATOR_ID, Buzz.class, Long.class);
    }

    public void addReadRecipient(String buzzId, Long userId) {
        Query query = new Query(Criteria.where("id").is(buzzId));
        Update update = new Update().addToSet(BuzzField.READ_RECIPIENTS, userId);
        mongoTemplate.updateFirst(query, update, Buzz.class);
    }
}
