package com.example.DunbarHorizon.social.adapter.out.neo4j.schema;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SocialUserSchemaInitializer {

    private final Neo4jClient neo4jClient;

    @PostConstruct
    public void createConstraints() {
        try {
            neo4jClient.query(
                    "CREATE CONSTRAINT social_user_id_unique IF NOT EXISTS " +
                    "FOR (s:SocialUser) REQUIRE s.id IS UNIQUE"
            ).run();
            log.info("[SocialUserSchemaInitializer] Unique constraint on SocialUser.id ensured.");
        } catch (Exception e) {
            log.warn("[SocialUserSchemaInitializer] Could not create constraint: {}", e.getMessage());
        }
    }
}
