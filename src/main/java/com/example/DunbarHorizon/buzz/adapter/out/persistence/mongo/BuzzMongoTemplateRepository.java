package com.example.DunbarHorizon.buzz.adapter.out.persistence.mongo;

import com.example.DunbarHorizon.buzz.domain.Buzz;
import com.example.DunbarHorizon.buzz.domain.BuzzReply;
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

    public void addReply(String buzzId, BuzzReply response) {
        Query query = new Query(Criteria.where("id").is(buzzId));
        Update update = new Update()
                .push(BuzzField.RESPONSES, response)
                .addToSet(BuzzField.READ_RECIPIENTS, response.getReplierId());

        mongoTemplate.updateFirst(query, update, Buzz.class);
    }

    public void updateReply(String buzzId, String responseId, String text, List<String> imageUrls) {
        Query query = new Query(Criteria.where("id").is(buzzId)
                .and(BuzzField.RESPONSES + "." + BuzzField.RESPONSE_ID).is(responseId));

        Update update = new Update()
                .set(BuzzField.RESPONSES + ".$." + BuzzField.TEXT, text)
                .set(BuzzField.RESPONSES + ".$." + BuzzField.IMAGE_URLS, imageUrls);

        mongoTemplate.updateFirst(query, update, Buzz.class);
    }

    public void removeReply(String buzzId, String responseId) {
        Query query = new Query(Criteria.where("id").is(buzzId));
        Update update = new Update().pull(BuzzField.RESPONSES,
                Query.query(Criteria.where(BuzzField.RESPONSE_ID).is(responseId)));

        mongoTemplate.updateFirst(query, update, Buzz.class);
    }

    public List<Long> findUnreadSenderIds(Long userId, Set<Long> mutedIds) {
        // 1. 내가 수신자 리스트에 있고 2. 내가 읽은 리스트에는 없으며 3. 차단하지 않은 유저의 캐스트 조회
        Criteria criteria = Criteria.where(BuzzField.RECIPIENT_IDS).is(userId)
                .and(BuzzField.READ_RECIPIENTS).ne(userId)
                .and(BuzzField.CREATOR_ID).nin(mutedIds);

        // creatorId만 추출하여 중복 제거(distinct)
        return mongoTemplate.findDistinct(new Query(criteria), BuzzField.CREATOR_ID, Buzz.class, Long.class);
    }

    public void addReadRecipient(String buzzId, Long userId) {
        Query query = new Query(Criteria.where("id").is(buzzId));

        Update update = new Update()
                // $addToSet: 배열에 데이터가 없을 때만 추가
                .addToSet("readRecipientIds", userId);

        mongoTemplate.updateFirst(query, update, Buzz.class);
    }
}