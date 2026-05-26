package com.example.DunbarHorizon.social.adapter.out.neo4j.springData;

import com.example.DunbarHorizon.social.domain.label.Label;
import lombok.NonNull;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.example.DunbarHorizon.social.domain.label.constant.LabelConstants.ATTACHED_TO;
import static com.example.DunbarHorizon.social.domain.label.constant.LabelConstants.LABEL;
import static com.example.DunbarHorizon.social.domain.label.constant.LabelConstants.OWNS_LABEL;
import static com.example.DunbarHorizon.social.domain.socialUser.constant.SocialUserConstants.USER_REFERENCE;

public interface LabelNeo4jRepository extends Neo4jRepository<Label, String> {

    @NonNull
    Optional<Label> findById(String id);

    boolean existsByOwner_IdAndLabelName(Long ownerId, String labelName);

    List<Label> findAllByOwner_Id(Long ownerId);

    @Query("MATCH (owner:" + USER_REFERENCE + " {id: $ownerId})-[:" + OWNS_LABEL + "]->(label:" + LABEL + ")" +
            "-[:" + ATTACHED_TO + "]->(member:" + USER_REFERENCE + " {id: $memberId}) " +
            "RETURN label")
    List<Label> findLabelsByOwnerAndMember(
            @Param("ownerId") Long ownerId,
            @Param("memberId") Long memberId
    );

    @Query("MATCH (owner:" + USER_REFERENCE + " {id: $ownerId})-[:" + OWNS_LABEL + "]->(label:" + LABEL + ")" +
            "-[:" + ATTACHED_TO + "]->(member:" + USER_REFERENCE + ") " +
            "WHERE label.id IN $labelIds " +
            "RETURN DISTINCT member.id")
    Set<Long> findMemberIdsByOwnerAndLabelIds(
            @Param("ownerId") Long ownerId,
            @Param("labelIds") List<String> labelIds
    );
}
