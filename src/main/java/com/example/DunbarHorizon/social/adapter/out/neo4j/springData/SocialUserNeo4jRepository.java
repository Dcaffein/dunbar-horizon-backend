package com.example.DunbarHorizon.social.adapter.out.neo4j.springData;

import com.example.DunbarHorizon.social.domain.socialUser.UserReference;
import com.example.DunbarHorizon.social.domain.socialUser.SocialUser;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Set;

import static com.example.DunbarHorizon.social.domain.socialUser.constant.SocialUserConstants.SOCIAL_USER;

public interface SocialUserNeo4jRepository extends Neo4jRepository<SocialUser, Long> {

    @Query("MATCH (u:" + SOCIAL_USER + ") WHERE u.id IN $ids RETURN u")
    Set<UserReference> findAllUserReferencesById(@Param("ids") Collection<Long> ids);
}
