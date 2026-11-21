package com.example.DunbarHorizon.buzz.adapter.out.persistence.legacy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.net.URI;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class LegacyBuzzMigrator implements ApplicationRunner {

    private static final String COLLECTION = "buzzes";

    private final MongoTemplate mongoTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final PlatformTransactionManager transactionManager;
    private final ObjectMapper objectMapper;

    @Override
    public void run(ApplicationArguments args) {
        int migrated = migrate(LocalDateTime.now());
        if (migrated > 0) {
            log.info("[LegacyBuzzMigrator] MongoDB의 미만료 버즈 {}건을 MySQL로 옮겼습니다.", migrated);
        }
    }

    public int migrate(LocalDateTime now) {
        Query unexpired = Query.query(Criteria.where("expiresAt").gt(toDate(now)));
        List<Document> documents = mongoTemplate.find(unexpired, Document.class, COLLECTION);
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        int migrated = 0;
        for (Document document : documents) {
            Boolean inserted = transactionTemplate.execute(status -> insertIfAbsent(document));
            mongoTemplate.remove(Query.query(Criteria.where("_id").is(document.get("_id"))), COLLECTION);
            if (Boolean.TRUE.equals(inserted)) {
                migrated++;
            }
        }
        return migrated;
    }

    private boolean insertIfAbsent(Document document) {
        String buzzId = document.getObjectId("_id").toHexString();
        int inserted = jdbcTemplate.update("""
                        INSERT INTO buzzes (id, creator_id, text, image_keys, created_at, expires_at)
                        SELECT ?, ?, ?, ?, ?, ? FROM DUAL
                        WHERE NOT EXISTS (SELECT 1 FROM buzzes WHERE id = ?)
                        """,
                buzzId,
                ((Number) document.get("creatorId")).longValue(),
                document.getString("text"),
                toJson(imageKeysOf(document.getList("imageUrls", String.class))),
                Timestamp.valueOf(toLocalDateTime(document.getDate("createdAt"))),
                Timestamp.valueOf(toLocalDateTime(document.getDate("expiresAt"))),
                buzzId);
        if (inserted == 0) {
            return false;
        }

        Set<Long> readUserIds = new HashSet<>(userIdsOf(document.getList("readRecipientIds", Number.class)));
        jdbcTemplate.batchUpdate(
                "INSERT INTO buzz_audience_members (buzz_id, user_id, is_read) VALUES (?, ?, ?)",
                userIdsOf(document.getList("recipientIds", Number.class)).stream()
                        .map(userId -> new Object[]{buzzId, userId, readUserIds.contains(userId)})
                        .toList());
        return true;
    }

    static List<String> imageKeysOf(List<String> imageUrls) {
        if (imageUrls == null) {
            return List.of();
        }
        return imageUrls.stream()
                .map(LegacyBuzzMigrator::imageKeyOf)
                .filter(key -> key != null && !key.isBlank())
                .toList();
    }

    private static String imageKeyOf(String imageUrl) {
        if (imageUrl == null || !imageUrl.startsWith("https://")) {
            return imageUrl;
        }
        String path = URI.create(imageUrl).getPath();
        return path.startsWith("/") ? path.substring(1) : path;
    }

    private static Set<Long> userIdsOf(Collection<Number> userIds) {
        Set<Long> ids = new LinkedHashSet<>();
        if (userIds != null) {
            userIds.forEach(userId -> ids.add(userId.longValue()));
        }
        return ids;
    }

    private String toJson(List<String> imageKeys) {
        try {
            return objectMapper.writeValueAsString(imageKeys);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("이미지 키를 JSON으로 바꾸지 못했습니다.", e);
        }
    }

    private static Date toDate(LocalDateTime dateTime) {
        return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    private static LocalDateTime toLocalDateTime(Date date) {
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }
}
