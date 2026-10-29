package com.example.DunbarHorizon.buzz.adapter.out.persistence.mongo;

import com.example.DunbarHorizon.buzz.domain.Buzz;
import com.example.DunbarHorizon.buzz.domain.BuzzComment;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class BuzzMongoTemplateRepository {

    private final MongoTemplate mongoTemplate;

    public void addComment(String buzzId, BuzzComment comment, boolean markCommenterAsRead) {
        Query query = new Query(Criteria.where("id").is(buzzId));
        Update update = new Update().push(BuzzField.RESPONSES, comment);
        if (markCommenterAsRead) {
            update.addToSet(BuzzField.READ_RECIPIENTS, comment.getCommenterId());
        }

        mongoTemplate.updateFirst(query, update, Buzz.class);
    }

    public void updateComment(String buzzId, String commentId, String text, List<String> imageUrls) {
        Query query = new Query(Criteria.where("id").is(buzzId)
                .and(BuzzField.RESPONSES + "." + BuzzField.RESPONSE_ID).is(commentId));

        Update update = new Update()
                .set(BuzzField.RESPONSES + ".$." + BuzzField.TEXT, text)
                .set(BuzzField.RESPONSES + ".$." + BuzzField.IMAGE_URLS, imageUrls);

        mongoTemplate.updateFirst(query, update, Buzz.class);
    }

    public void removeComment(String buzzId, String commentId) {
        Query query = new Query(Criteria.where("id").is(buzzId));
        Update update = new Update().pull(BuzzField.RESPONSES,
                Query.query(Criteria.where(BuzzField.RESPONSE_ID).is(commentId)));

        mongoTemplate.updateFirst(query, update, Buzz.class);
    }

    public List<Long> findUnreadSenderIds(Long userId, Set<Long> mutedIds) {
        Criteria criteria = Criteria.where(BuzzField.RECIPIENT_IDS).is(userId)
                .and(BuzzField.READ_RECIPIENTS).ne(userId)
                .and(BuzzField.CREATOR_ID).nin(mutedIds);

        return mongoTemplate.findDistinct(new Query(criteria), BuzzField.CREATOR_ID, Buzz.class, Long.class);
    }

    public void addReadRecipient(String buzzId, Long userId) {
        Query query = new Query(Criteria.where("id").is(buzzId));
        Update update = new Update().addToSet("readRecipientIds", userId);
        mongoTemplate.updateFirst(query, update, Buzz.class);
    }

    public Buzz findByIdWithCommentSlice(String buzzId) {
        Query query = new Query(Criteria.where("_id").is(buzzId));
        query.fields().slice(BuzzField.RESPONSES, -20);
        return mongoTemplate.findOne(query, Buzz.class);
    }

    public Buzz findByIdWithoutComments(String buzzId) {
        Query query = new Query(Criteria.where("_id").is(buzzId));
        query.fields().exclude(BuzzField.RESPONSES);
        return mongoTemplate.findOne(query, Buzz.class);
    }

    public Optional<BuzzComment> findCommentById(String buzzId, String commentId) {
        Query query = new Query(Criteria.where("_id").is(buzzId));
        query.fields().elemMatch(BuzzField.RESPONSES, Criteria.where(BuzzField.RESPONSE_ID).is(commentId));

        Buzz buzz = mongoTemplate.findOne(query, Buzz.class);
        if (buzz == null || buzz.getComments().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(buzz.getComments().get(0));
    }
}
