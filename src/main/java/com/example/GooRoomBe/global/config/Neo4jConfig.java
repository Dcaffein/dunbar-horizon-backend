package com.example.GooRoomBe.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.EnableNeo4jAuditing;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableNeo4jAuditing // Neo4j Auditing 활성화
@EnableTransactionManagement // 트랜잭션 관리 활성화
@EnableNeo4jRepositories(
        basePackages = {
                "com.example.GooRoomBe.social" // Friendship(Neo4j)이 여기 있음
        }
)
public class Neo4jConfig {
    // Neo4j 관련 커스텀 설정이 필요하면 여기에 작성
}