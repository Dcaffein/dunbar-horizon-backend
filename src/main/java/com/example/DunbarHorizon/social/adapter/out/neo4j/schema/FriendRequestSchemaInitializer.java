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
                    "CREATE CONSTRAINT friend_request_id_unique IF NOT EXISTS " +
                    "FOR (r:FriendRequest) REQUIRE r.id IS UNIQUE"
            ).run();
            log.info("[FriendRequestSchemaInitializer] Unique constraint on FriendRequest.id ensured.");
        } catch (Exception e) {
            log.warn("[FriendRequestSchemaInitializer] Could not create constraint: {}", e.getMessage());
        }

        migrateCreatedAtToLocalDateTime();
    }

    // DATE_TIME(timezone 포함)으로 저장된 레거시 createdAt을 LOCAL_DATE_TIME으로 변환
    private void migrateCreatedAtToLocalDateTime() {
        try {
            var result = neo4jClient.query(
                    "MATCH (fr:FriendRequest) " +
                    "WHERE fr.createdAt IS NOT NULL AND toString(fr.createdAt) CONTAINS 'Z' " +
                    "SET fr.createdAt = localdatetime(fr.createdAt) " +
                    "RETURN count(fr) AS migrated"
            ).fetchAs(Long.class).mappedBy((typeSystem, record) -> record.get("migrated").asLong()).one();

            result.ifPresent(count -> {
                if (count > 0) {
                    log.info("[FriendRequestSchemaInitializer] Migrated {} FriendRequest.createdAt from DATE_TIME to LOCAL_DATE_TIME.", count);
                }
            });
        } catch (Exception e) {
            log.warn("[FriendRequestSchemaInitializer] createdAt migration failed: {}", e.getMessage());
        }
    }
}
