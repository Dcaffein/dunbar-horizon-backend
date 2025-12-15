package com.example.GooRoomBe.account.auth.repository;

import com.example.GooRoomBe.account.auth.domain.token.RefreshToken;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends Neo4jRepository<RefreshToken, String> {
    Optional<RefreshToken> findByUser_IdAndTokenValue(String userId, String tokenValue);
    boolean existsByUser_IdAndTokenValue(String userId, String tokenValue);
    void deleteByUser_IdAndTokenValue(String userId, String tokenValue);
    void deleteAllByUser_Id(String userId);
}
