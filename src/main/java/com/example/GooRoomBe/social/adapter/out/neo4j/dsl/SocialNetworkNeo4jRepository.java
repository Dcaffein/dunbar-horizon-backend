package com.example.GooRoomBe.social.adapter.out.neo4j.dsl;

import com.example.GooRoomBe.social.application.dto.NetworkOneHopsByTwoHopResponse;
import com.example.GooRoomBe.social.application.dto.NetworkTwoHopSuggestionsResponse;
import com.example.GooRoomBe.social.application.dto.NetworkBetweenOneHopsResponse;
import com.example.GooRoomBe.social.application.port.out.SocialNetworkRepository;
import lombok.RequiredArgsConstructor;
import org.neo4j.cypherdsl.core.*;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.example.GooRoomBe.social.domain.friend.constant.FriendConstants.MEMBER_OF;
import static com.example.GooRoomBe.social.adapter.out.neo4j.dsl.SocialNetworkPatterns.*;
import static com.example.GooRoomBe.social.adapter.out.neo4j.dsl.SocialNetworkVariables.*;
import static com.example.GooRoomBe.social.domain.label.constant.LabelConstants.HAS_MEMBER;
import static org.neo4j.cypherdsl.core.Cypher.*;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SocialNetworkNeo4jRepository implements SocialNetworkRepository {

    private final Neo4jTemplate neo4jTemplate;

    /**
     * 현재 사용자의 1-Hop 친구 목록과 그들 사이의 상호 친구 정보를 상세 조회
     * <p>요구사항: 1. 나의 관심도(interestScore), 2. 우리 사이의 친밀도(intimacy), 3. 상호 친구간 친밀도 포함</p>
     */
    public List<NetworkBetweenOneHopsResponse> getOneHopsNetwork(Long userId) {
        Node me = user().named("me");
        Node oneHopFriend = user().named("oneHopFriend");
        Node mutualFriend = user().named("mutualFriend");
        Node oneHopFriendship = friendship().named("oneHopFriendship");
        Node sharedFriendship = friendship().named("sharedFriendship");

        // 1번 요구사항(나의 관심도)을 위해 나로부터 나가는 관계를 'myRel'로 정의
        Relationship myRel = me.relationshipTo(oneHopFriendship, MEMBER_OF).named("myRel");
        SymbolicName oneHopFriendIds = name("oneHopFriendIds");

        Statement statement = matchUserWithId(userId, "me")
                // [Step 1] 친구 ID 풀 수집
                // 상호 친구 여부를 판단하기 위해, 먼저 내 모든 친구들의 ID를 수집하여 리스트로 보관
                .match(friendshipBetween(me, friendship().named("lookupFriendship"), user().named("lookupUser")))
                .with(me, collect(user().named("lookupUser").property(PROP_ID)).as(oneHopFriendIds))

                // [Step 2] 친구 관계 및 친밀도 정보 매칭
                // 1번(interestScore)과 2번(intimacy)을 위해 관계(myRel)와 노드(oneHopFriendship)를 모두 매칭
                .match(myRel.relationshipFrom(oneHopFriend, MEMBER_OF))
                .with(me, oneHopFriend, oneHopFriendship, myRel, oneHopFriendIds)

                // [Step 3] 상호 친구 및 그들 사이의 친밀도 식별 (2-Hop 내측 탐색)
                // 3번(oneHop간 intimacy)을 위해 친구들 사이의 'sharedFriendship' 노드를 함께 매칭
                .optionalMatch(friendshipBetween(oneHopFriend, sharedFriendship, mutualFriend))
                .where(mutualFriend.property(PROP_ID).in(oneHopFriendIds))
                .and(isSameNode(mutualFriend, oneHopFriend).not())

                // [Step 4] 데이터 집계 및 반환
                // 상호 친구 정보는 ID와 그들 사이의 친밀도를 맵(Map)으로 묶어서 리스트로 수집
                .with(oneHopFriend, oneHopFriendship, myRel,
                        collectDistinct(
                                mapOf(
                                        "mutualFriendId", mutualFriend.property(PROP_ID),
                                        "mutualIntimacy", sharedFriendship.property(PROP_INTIMACY)
                                )
                        ).as("mutualFriendDetails"))
                .returning(
                        oneHopFriend.property(PROP_ID).as("friendId"),
                        oneHopFriend.property(PROP_NICKNAME).as("friendName"),
                        oneHopFriendship.property(PROP_FRIEND_ALIAS).as("friendAlias"),
                        myRel.property(PROP_INTEREST_SCORE).as("myInterestScore"),
                        oneHopFriendship.property(PROP_INTIMACY).as("meAndFriendIntimacy"),
                        name("mutualFriendDetails")
                ).build();

        return neo4jTemplate.findAll(statement, NetworkBetweenOneHopsResponse.class);
    }

    /**
     * 특정 피벗 친구(pivotId)를 통해, 해당 피벗과 관련이 깊은 2 hop 유저를 친구로 추천
     * <p><b>정렬:</b> 피벗과 타겟 사이의 {@code intimacy} 기준 내림차순</p>
     */
    @Override
    public List<NetworkTwoHopSuggestionsResponse> getTwoHopSuggestionsByOneHop(Long userId, Long pivotId) {
        Node me = user().named("me");
        Node pivot = user().named("pivot");
        Node target = user().named("target");
        Node commonFriend = user().named("commonFriend");
        Node label = label().named("label");
        Node fsNode = friendship().named("fs1");
        Node targetFsNode = friendship().named("pfs");

        Relationship myRel = me.relationshipTo(fsNode, MEMBER_OF).named("myRel");
        SymbolicName exclusionIds = name("exclusionIds");

        Statement statement = matchUserWithId(userId, "me")
                // 제외 리스트 수집 (나 + 내 현재 친구들)
                .match(friendshipBetween(me, friendship().named("scan"), user().named("ex")))
                .with(me, collect(user().named("ex").property(PROP_ID)).as(exclusionIds))

                // 피벗과의 통로(isRoutable) 확인
                .match(myRel.relationshipFrom(pivot, MEMBER_OF))
                .where(idEquals(pivot, pivotId).and(isRoutable(myRel)))

                //  피벗의 친구들 탐색
                .match(friendshipBetween(pivot, targetFsNode, target))

                // 이전에 수집한 제외 리스트에 포함된 나와 나의 친구들은 제외
                .where(isNotSelfAndNotExcluded(target, me, exclusionIds))

                .with(me, pivot, target, targetFsNode,
                        // 공통 라벨 존재 여부 확인
                        exists(
                                ownsLabel(pivot, label)
                                        .relationshipTo(target, HAS_MEMBER)
                                        .relationshipFrom(me, HAS_MEMBER)
                        ).as("hasCommonLabel"),

                        // 피벗을 제외한 추가 상호 친구 존재 여부 확인
                        exists(
                                friendshipBetween(me, friendship().named("f1"), commonFriend)
                                        .relationshipTo(friendship().named("f2"), MEMBER_OF)
                                        .relationshipFrom(target, MEMBER_OF)
                                        .where(isNotSameNode(commonFriend, pivot))
                        ).as("hasAnotherMutualFriend")
                )

                .where(name("hasCommonLabel").isEqualTo(literalOf(true))
                        .or(name("hasAnotherMutualFriend").isEqualTo(literalOf(true))))


                // 수집된 2 hop 친구들 중에서 INTIMACY 상위 10명 반환
                .returningDistinct(
                        target.property(PROP_ID).as("suggestedFriendId"),
                        target.property(PROP_NICKNAME).as("suggestedFriendName"),
                        pivot.property(PROP_ID).as("commonFriendId")
                )
                .orderBy(targetFsNode.property(PROP_INTIMACY).descending())
                .limit(10)
                .build();

        return neo4jTemplate.findAll(statement, NetworkTwoHopSuggestionsResponse.class);
    }

    /**
     * 특정 2-Hop 유저(targetUser)와 나 사이에 겹치는 친구(oneHopFriend) 목록을 조회
     */
    public List<NetworkOneHopsByTwoHopResponse> getIntersectionOneHops(Long userId, Long targetId) {
        Node me = user().named("me");
        Node targetUser = user().named("targetUser");
        Node oneHopFriend = user().named("oneHopFriend");

        Statement statement = matchUserWithId(userId, "me")
                // 경로 매칭
                // (나)-(친구)-(타겟) 구조가 성립하는 모든 oneHop 친구들을 찾아 그들의 ID를 반환
                .match(friendshipBetween(me, friendship().named("myFriendship"), oneHopFriend))
                .match(friendshipBetween(oneHopFriend, friendship().named("targetFriendship"), targetUser))
                .where(idEquals(targetUser, targetId))

                .returning(oneHopFriend.property(PROP_ID).as("friendId"))
                .build();

        return neo4jTemplate.findAll(statement, NetworkOneHopsByTwoHopResponse.class);
    }

    /**
     * 특정 피벗 친구를 중심으로 가장 관련성이 높은 유저들을 조회
     * <p><b>기준:</b> 공통 라벨이 있거나, 나-타겟 사이에 공통 친구가 2명 이상인 경우</p>
     * <p><b>참고:</b> 추천이 아닌 '관계 조회' 목적이므로 기존 친구 제외 로직은 포함하지 않음</p>
     */
    @Override
    public Set<Long> getRelatedNetworkByPivot(Long userId, Long pivotFriendId, int limitCount) {

        Node me = user().named("me");
        Node pivot = user().named("pivot");
        Node target = user().named("target");
        Node commonFriend = user().named("commonFriend");
        Node label = label().named("label");
        Node fsNode = friendship().named("fs1");
        Node targetFsNode = friendship().named("pfs");

        Relationship myRel = me.relationshipTo(fsNode, MEMBER_OF).named("myRel");

        Statement statement = matchUserWithId(userId, "me")
                // 피벗 연결 확인
                // 피벗과의 통로(isRoutable) 확인
                .match(myRel.relationshipFrom(pivot, MEMBER_OF))
                .where(idEquals(pivot, pivotFriendId))
                .and(isRoutable(myRel))

                // 피벗의 지인들 탐색
                // 피벗과 우정 관계를 맺고 있는 모든 타겟 유저들을 일차적으로 매칭
                .match(friendshipBetween(pivot, targetFsNode, target))

                // 나 자신은 타겟에서 제외
                .where(isNotSameNode(target, me))

                // 각 target 마다 공통 라벨 존재 여부와 '피벗 외 추가 상호 친구' 존재 여부를 확인
                .with(target, targetFsNode,
                        // (me)-[:OWNS]-(label)-[:HAS_MEMBER]-(target) 경로 존재 여부
                        exists(
                                ownsLabel(pivot, label)
                                        .relationshipTo(target, HAS_MEMBER)
                                        .relationshipFrom(me, HAS_MEMBER)
                        ).as("hasCommonLabel"),

                        // 피벗을 제외한 또 다른 상호 친구가 존재하는지 확인
                        exists(
                                friendshipBetween(me, friendship().named("f1"), commonFriend)
                                        .relationshipTo(friendship().named("f2"), MEMBER_OF)
                                        .relationshipFrom(target, MEMBER_OF)
                                        .where(isNotSameNode(commonFriend, pivot))
                        ).as("hasAnotherMutualFriend")
                )

                // 공통 라벨이 존재하거나, 피벗 외의 상호 친구가 최소 한 명 더 있는 경우
                .where(name("hasCommonLabel").isEqualTo(literalOf(true))
                        .or(name("hasAnotherMutualFriend").isEqualTo(literalOf(true))))

                // intimacy 기준으로 정렬해서 limitCount 만큼 반환
                .returning(target.property(PROP_ID).as("targetId"))
                .orderBy(targetFsNode.property(PROP_INTIMACY).descending())
                .limit(limitCount)
                .build();

        return new HashSet<>(neo4jTemplate.findAll(statement, Long.class));
    }


    /**
     * 내 친구(oneHop)를 통해 알 수 있는 '내가 아직 모르는 유저'를 2-Hop 기반으로 추천
     * <p><b>Cypher:</b> {@code WHERE target.id <> me.id AND NOT (target.id IN $exclusionIds)}</p>
     */
    private List<NetworkTwoHopSuggestionsResponse> getTwoHopSuggestions(Long userId) {
        Node me = user().named("me");
        Node oneHop = user().named("oneHop");
        Node target = user().named("target");
        Node label = label().named("label");
        Node fsNode = friendship().named("fs1");

        Relationship myRel = me.relationshipTo(fsNode, MEMBER_OF).named("myRel");
        SymbolicName exclusionIds = name("exclusionIds");

        Statement statement = matchUserWithId(userId, "me")
                // 제외 리스트 생성
                // 추천 결과에서 나 자신과 현재 이미 친구인 사람들을 걸러내기 위해 ID 리스트를 확보
                .match(friendshipBetween(me, friendship().named("scan"), user().named("ex")))
                .with(me, collect(user().named("ex").property(PROP_ID)).as(exclusionIds))

                // 통로 확인
                // 내 친구 관계 중 '라우팅(Routable)' 설정이 허용된 통로만 타고 넘어감
                .match(myRel.relationshipFrom(oneHop, MEMBER_OF))
                .where(isRoutable(myRel))
                .and(isNotSameNode(oneHop, me))

                // 공유 라벨 탐색
                // 친구(oneHop)가 소유한 라벨 중 외부 노출이 허용된(exposure=true) 것을 찾고, 그 라벨에 나(me)와 타겟(target)이 공통 멤버인지 확인
                .match(ownsLabel(oneHop, label))
                .where(label.property(PROP_EXPOSURE).isTrue())
                .match(hasMember(label, me))
                .match(hasMember(label, target))

                // 최종 필터링
                // 발견된 타겟이 나 자신도 아니고, 이미 내 친구 목록에도 없는 '순수한 타인'인지 확인
                .with(me, oneHop, target, exclusionIds)
                .where(isNotSelfAndNotExcluded(target, me, exclusionIds))

                .returningDistinct(
                        target.property(PROP_ID).as("suggestedFriendId"),
                        target.property(PROP_NICKNAME).as("suggestedFriendName"),
                        oneHop.property(PROP_ID).as("commonFriendId")
                ).build();

        return neo4jTemplate.findAll(statement, NetworkTwoHopSuggestionsResponse.class);
    }
}