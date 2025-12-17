package com.example.GooRoomBe.global.userReference;

import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface SocialUserRepository extends Neo4jRepository<SocialUser, String> {
}
