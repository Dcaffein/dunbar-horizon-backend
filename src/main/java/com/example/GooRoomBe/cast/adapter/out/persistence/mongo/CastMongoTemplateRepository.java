package com.example.GooRoomBe.cast.adapter.out.persistence.mongo;

import com.example.GooRoomBe.cast.domain.model.Cast;
import com.example.GooRoomBe.cast.domain.model.CastReply;
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
public class CastMongoTemplateRepository {

    private final MongoTemplate mongoTemplate;

    public void addReply(String castId, CastReply response) {
        Query query = new Query(Criteria.where("id").is(castId));
        Update update = new Update()
                .push(CastField.RESPONSES, response)
                .addToSet(CastField.READ_RECIPIENTS, response.getReplierId());

        mongoTemplate.updateFirst(query, update, Cast.class);
    }

    public void updateReply(String castId, String responseId, String text, List<String> imageUrls) {
        Query query = new Query(Criteria.where("id").is(castId)
                .and(CastField.RESPONSES + "." + CastField.RESPONSE_ID).is(responseId));

        Update update = new Update()
                .set(CastField.RESPONSES + ".$." + CastField.TEXT, text)
                .set(CastField.RESPONSES + ".$." + CastField.IMAGE_URLS, imageUrls);

        mongoTemplate.updateFirst(query, update, Cast.class);
    }

    public void removeReply(String castId, String responseId) {
        Query query = new Query(Criteria.where("id").is(castId));
        Update update = new Update().pull(CastField.RESPONSES,
                Query.query(Criteria.where(CastField.RESPONSE_ID).is(responseId)));

        mongoTemplate.updateFirst(query, update, Cast.class);
    }

    public List<Long> findUnreadSenderIds(Long userId, Set<Long> mutedIds) {
        // 1. 내가 수신자 리스트에 있고 2. 내가 읽은 리스트에는 없으며 3. 차단하지 않은 유저의 캐스트 조회
        Criteria criteria = Criteria.where(CastField.RECIPIENT_IDS).is(userId)
                .and(CastField.READ_RECIPIENTS).ne(userId)
                .and(CastField.CREATOR_ID).nin(mutedIds);

        // creatorId만 추출하여 중복 제거(distinct)
        return mongoTemplate.findDistinct(new Query(criteria), CastField.CREATOR_ID, Cast.class, Long.class);
    }
}