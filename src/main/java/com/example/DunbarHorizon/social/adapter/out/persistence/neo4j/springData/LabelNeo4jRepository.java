package com.example.DunbarHorizon.social.adapter.out.persistence.neo4j.springData;

import com.example.DunbarHorizon.social.domain.label.Label;
import lombok.NonNull;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.example.DunbarHorizon.social.domain.label.constant.LabelConstants.ATTACHED_TO;
import static com.example.DunbarHorizon.social.domain.label.constant.LabelConstants.LABEL;
import static com.example.DunbarHorizon.social.domain.label.constant.LabelConstants.OWNS_LABEL;
import static com.example.DunbarHorizon.social.domain.socialUser.constant.SocialUserConstants.SOCIAL_USER;
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

    @Query("MATCH (owner:" + USER_REFERENCE + " {id: $ownerId}) " +
            "CREATE (owner)-[:" + OWNS_LABEL + "]->(label:" + LABEL + ") " +
            "SET label += $properties " +
            "RETURN count(label)")
    long create(@Param("ownerId") Long ownerId, @Param("properties") Map<String, Object> properties);

    @Query("MATCH (label:" + LABEL + " {id: $labelId}) SET label.labelName = $labelName")
    void rename(@Param("labelId") String labelId, @Param("labelName") String labelName);

    @Query("RETURN EXISTS { MATCH (:" + LABEL + " {id: $labelId})-[:" + ATTACHED_TO + "]->" +
            "(:" + USER_REFERENCE + " {id: $memberId}) }")
    boolean existsMember(@Param("labelId") String labelId, @Param("memberId") Long memberId);

    @Query("MATCH (label:" + LABEL + " {id: $labelId}), (member:" + USER_REFERENCE + " {id: $memberId}) " +
            "MERGE (label)-[:" + ATTACHED_TO + "]->(member) " +
            "RETURN count(member)")
    long addMember(@Param("labelId") String labelId, @Param("memberId") Long memberId);

    @Query("MATCH (:" + LABEL + " {id: $labelId})-[attached:" + ATTACHED_TO + "]->" +
            "(:" + USER_REFERENCE + " {id: $memberId}) " +
            "DELETE attached")
    void removeMember(@Param("labelId") String labelId, @Param("memberId") Long memberId);

    @Query("MATCH (label:" + LABEL + " {id: $labelId}) " +
            "SET label._memberLock = true " +
            "REMOVE label._memberLock " +
            "WITH label " +
            "OPTIONAL MATCH (label)-[attached:" + ATTACHED_TO + "]->(current:" + USER_REFERENCE + ") " +
            "WHERE NOT current.id IN $memberIds " +
            "DELETE attached " +
            "WITH DISTINCT label " +
            "UNWIND $memberIds AS memberId " +
            "MATCH (member:" + USER_REFERENCE + " {id: memberId}) " +
            "MERGE (label)-[:" + ATTACHED_TO + "]->(member)")
    void replaceMembers(@Param("labelId") String labelId, @Param("memberIds") Collection<Long> memberIds);

    // 친구 관계가 끝났으므로 비활성 여부와 관계없이 멤버십을 지운다.
    @Query("MATCH (:" + SOCIAL_USER + " {id: $ownerId})-[:" + OWNS_LABEL + "]->(:" + LABEL + ")" +
            "-[attached:" + ATTACHED_TO + "]->(:" + SOCIAL_USER + " {id: $memberId}) " +
            "DELETE attached")
    void removeMemberFromOwnedLabels(@Param("ownerId") Long ownerId, @Param("memberId") Long memberId);

    @Query("MATCH (owner:" + USER_REFERENCE + " {id: $ownerId})-[:" + OWNS_LABEL + "]->(label:" + LABEL + ")" +
            "-[:" + ATTACHED_TO + "]->(member:" + USER_REFERENCE + ") " +
            "WHERE label.id IN $labelIds " +
            "RETURN DISTINCT member.id")
    Set<Long> findMemberIdsByOwnerAndLabelIds(
            @Param("ownerId") Long ownerId,
            @Param("labelIds") List<String> labelIds
    );
}
