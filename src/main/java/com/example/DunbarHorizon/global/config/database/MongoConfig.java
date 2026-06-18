package com.example.DunbarHorizon.global.config.database;

import com.example.DunbarHorizon.buzz.domain.Buzz;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(
        basePackages = {"com.example.DunbarHorizon.notification.adapter.out.persistence.mongo",
                "com.example.DunbarHorizon.buzz.adapter.out.persistence.mongo"}
)
@EnableMongoAuditing
@RequiredArgsConstructor
public class MongoConfig {

    private final MongoTemplate mongoTemplate;

    // Spring Data가 expiresAt에 일반 인덱스를 먼저 생성하면 TTL 인덱스로 자동 교체되지 않으므로
    // 앱 시작 시 기존 인덱스를 드롭하고 TTL 인덱스로 재생성한다.
    @EventListener(ApplicationReadyEvent.class)
    public void ensureBuzzTtlIndex() {
        IndexOperations indexOps = mongoTemplate.indexOps(Buzz.class);
        try {
            indexOps.dropIndex("expiresAt");
        } catch (Exception ignored) {}
        indexOps.ensureIndex(new Index().on("expiresAt", Sort.Direction.ASC).expire(0));
    }
}