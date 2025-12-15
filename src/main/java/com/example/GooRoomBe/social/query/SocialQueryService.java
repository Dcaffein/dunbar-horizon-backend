package com.example.GooRoomBe.social.query;

import com.example.GooRoomBe.social.query.api.ConnectingFriendDto;
import com.example.GooRoomBe.social.query.api.FriendSuggestionDto;
import com.example.GooRoomBe.social.query.api.OneHopsNetworkDto;
import lombok.RequiredArgsConstructor;
import org.neo4j.cypherdsl.core.*;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.example.GooRoomBe.social.common.SocialSchemaConstants.HAS_MEMBER;
import static com.example.GooRoomBe.social.common.SocialSchemaConstants.OWNS;
import static com.example.GooRoomBe.social.query.Neo4jVariables.ME;
import static com.example.GooRoomBe.social.query.Neo4jVariables.ONE_HOP_FRIEND;
import static org.neo4j.cypherdsl.core.Cypher.*;

import static com.example.GooRoomBe.social.query.Neo4jVariables.*;
import static com.example.GooRoomBe.social.query.Neo4jConditions.*;
import static com.example.GooRoomBe.social.query.Neo4jPatterns.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SocialQueryService {

    private final Neo4jTemplate neo4jTemplate;

    /**
     * 현재 사용자의 친구 목록과 함께 아는 친구 정보를 조회
     */
    public List<OneHopsNetworkDto> getOneHopsNetwork(String userId) {

        //  1-hop 패턴 호출
        StatementBuilder.OngoingReadingAndWith firstWith = findOneHopFriends(literalOf(userId))
                .with(ME, ONE_HOP_FRIEND, ALIAS, MY_REL);

        // 2-hop(MUTUAL_FRIEND) 패턴 호출
        StatementBuilder.OngoingReadingAndWith secondWith = findTwoHopPatternOptional(firstWith, MUTUAL_FRIEND)
                .and(isFriends(ME, MUTUAL_FRIEND)) // 함께 아는 친구 조건
                .with(ME, ONE_HOP_FRIEND, ALIAS, MY_REL,MUTUAL_FRIEND);

        Statement statement = secondWith
                .with(
                        ONE_HOP_FRIEND,
                        ALIAS,
                        MY_REL,
                        collectDistinct(MUTUAL_FRIEND.property("id")).as("mutualFriendIds")
                )
                .returning(
                        ONE_HOP_FRIEND.property("id").as("friendId"),
                        ONE_HOP_FRIEND.property("nickname").as("friendName"),
                        ALIAS,
                        MY_REL.property("interactionScore").as("interactionScore"),
                        name("mutualFriendIds")
                )
                .build();

        return neo4jTemplate.findAll(statement, OneHopsNetworkDto.class);
    }

    /**
     * 2-hop 친구
     * 1-hop에 대한 onIntroduce : true
     * 1-hop 친구가 가진 라벨 중 내가 멤버인 라벨의 다른 멤버들을 2 hop 으로 소개
     */
    public List<FriendSuggestionDto> getTwoHopSuggestion(String userId) {

        Property myRelOnIntroduce = MY_REL.property("onIntroduce");
        Node labelNamed = LABEL_NAMED;

        // 1-hop 친구 찾기
        var firstWith = findOneHopFriends(literalOf(userId))
                .and(myRelOnIntroduce.isTrue())
                .with(ME, ONE_HOP_FRIEND);

        //  라벨 매칭 조건 강화
        var secondWith = firstWith
                // 친구가 소유한 라벨
                .match(labelNamed.relationshipFrom(ONE_HOP_FRIEND, OWNS))
                .where(labelNamed.property(PROP_EXPOSURE).isTrue())
                // 그 라벨에 me 포함
                .match(labelNamed.relationshipTo(ME, HAS_MEMBER))
                // 그 라벨의 다른 멤버(Target) 찾기
                .match(labelNamed.relationshipTo(TWO_HOP_FRIEND, HAS_MEMBER))

                .with(ME, ONE_HOP_FRIEND, TWO_HOP_FRIEND);

        Statement statement = secondWith
                .where(TWO_HOP_FRIEND.property("id").isNotEqualTo(ME.property("id")))
                .and(TWO_HOP_FRIEND.property("id").isNotEqualTo(ONE_HOP_FRIEND.property("id")))
                .and(isNotFriends(ME, TWO_HOP_FRIEND))
                .returningDistinct(
                        TWO_HOP_FRIEND.property("id").as("suggestedFriendId"),
                        TWO_HOP_FRIEND.property("nickname").as("suggestedFriendName"),
                        ONE_HOP_FRIEND.property("id").as("commonFriendId")
                )
                .build();

        return neo4jTemplate.findAll(statement, FriendSuggestionDto.class);
    }

    /**
     * 2-Hop 유저(Target)를 아는 내 친구 목록 조회
     *
     * @param targetId 2-hop 친구 ID
     * @param myFriendIds 내 친구들의 ID 목록 (후보군)
     * @return 연결고리가 되는 친구들의 ID 리스트 (List<ConnectingFriendDto>)
     */
    public List<ConnectingFriendDto> getConnectingFriends(String targetId, List<String> myFriendIds) {

        // 1. 노드 정의
        Node target = USER.named("target");
        Node candidate = USER.named("candidate");

        // 2. 쿼리 빌드
        // MATCH (target:User {id: $targetId})
        // MATCH (candidate:User)
        // WHERE candidate.id IN $myFriendIds
        // AND (target과 candidate가 친구 관계임)
        // RETURN candidate.id AS friendId
        Statement statement = match(target)
                .where(target.property("id").isEqualTo(literalOf(targetId)))
                .match(candidate)
                .where(candidate.property("id").in(literalOf(myFriendIds))) // 후보군(내 친구들) 안에서만 검색
                .and(isFriends(target, candidate)) // Neo4jConditions.isFriends 활용
                .returning(
                        // ⭐️ DTO 필드명("friendId")과 Alias를 일치시킴
                        candidate.property("id").as("friendId")
                )
                .build();

        // 3. 실행 (findAll 사용)
        // 쿼리 결과의 각 행(friendId)을 ConnectingFriendDto 객체로 자동 매핑하여 리스트로 반환합니다.
        return neo4jTemplate.findAll(statement, ConnectingFriendDto.class);
    }
}