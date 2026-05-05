package com.example.DunbarHorizon.social.adapter.out.neo4j.schema;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FriendRequestSchemaInitializer {

    private final Neo4jClient neo4jClient;

    @PostConstruct
    public void createConstraints() {
        try {
            neo4jClient.query(
                    "CREATE CONSTRAINT friend_request_pair_unique IF NOT EXISTS " +
                    "FOR (r:FriendRequest) REQUIRE r.pairKey IS UNIQUE"
            ).run();
            log.info("[FriendRequestSchemaInitializer] Unique constraint on FriendRequest.pairKey ensured.");
        } catch (Exception e) {
            log.warn("[FriendRequestSchemaInitializer] Could not create constraint: {}", e.getMessage());
        }
    }
}
