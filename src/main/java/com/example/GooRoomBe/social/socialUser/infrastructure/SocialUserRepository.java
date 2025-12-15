package com.example.GooRoomBe.social.socialUser.infrastructure;

import com.example.GooRoomBe.social.socialUser.SocialUser;
import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface SocialUserRepository extends Neo4jRepository<SocialUser, String> {
}
