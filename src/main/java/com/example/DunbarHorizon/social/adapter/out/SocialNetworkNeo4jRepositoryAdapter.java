package com.example.DunbarHorizon.social.adapter.out;

import com.example.DunbarHorizon.social.adapter.out.neo4j.dsl.ApocPatterns;
import com.example.DunbarHorizon.social.application.dto.result.MutualFriendEdgeResult;
import com.example.DunbarHorizon.social.application.dto.result.NetworkFriendEdgeResult;
import com.example.DunbarHorizon.social.application.dto.result.NetworkOneHopsByTwoHopResult;
import com.example.DunbarHorizon.social.application.port.out.SocialNetworkRepository;
import lombok.RequiredArgsConstructor;
import org.neo4j.cypherdsl.core.*;
import org.neo4j.cypherdsl.core.renderer.Configuration;
import org.neo4j.cypherdsl.core.renderer.Renderer;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

import static com.example.DunbarHorizon.social.adapter.out.neo4j.dsl.SocialNetworkPatterns.*;
import static com.example.DunbarHorizon.social.adapter.out.neo4j.dsl.SocialNetworkProperties.*;
import static com.example.DunbarHorizon.social.domain.friend.constant.FriendConstants.HAS_FRIENDSHIP;
import static org.neo4j.cypherdsl.core.Cypher.*;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SocialNetworkNeo4jRepositoryAdapter implements SocialNetworkRepository {

    private final Neo4jClient neo4jClient;

    private static final Renderer renderer = Renderer.getRenderer(Configuration.defaultConfig());

    /**
     * 내 친구들 간의 Edge List 반환
     */
    @Override
    public List<NetworkFriendEdgeResult> getFriendsNetwork(Long userId) {

        // 그래프 탐색을 위한 기준 사용자(me)와 친구(friend) 노드 정의
        Node me = user().named("me");
        Node friend = user().named("friend");

        // 경로 추출 및 ID 매핑을 위한 심볼릭 네임 정의
        SymbolicName friendList = name("friendList");
        SymbolicName idMap = name("idMap");
        SymbolicName path = name("path");
        SymbolicName pathNodes = name("pathNodes");

        // APOC 탐색 경로에서 노드 리스트를 추출하는 함수 정의
        Expression nodesOfPath = call("nodes").withArgs(path).asFunction();

        // 경로상의 [0:출발노드(User), 1:매개노드(Friendship), 2:도착노드(User)] 추출 매핑
        Expression friendANode = valueAt(pathNodes, 0);
        Expression middleFriendshipNode = valueAt(pathNodes, 1);
        Expression friendBNode = valueAt(pathNodes, 2);

        // idMap 메모리 룩업을 통해 내부 elementId를 비즈니스 ID로 변환하는 출력 공식 정의
        Expression friendAId = valueAt(idMap, call("elementId").withArgs(friendANode).asFunction());
        Expression friendBId = valueAt(idMap, call("elementId").withArgs(friendBNode).asFunction());

        // 관계 속성에서 친밀도를 추출하며 값이 없을 경우 0.0으로 대체하도록 명시
        Expression intimacy = coalesce(property(middleFriendshipNode, PROP_INTIMACY), literalOf(0.0));

        // 1. 사용자의 모든 친구를 수집하고, db 내부용 elementId -> businessId(property) 매핑 테이블(idMap) 생성
        StatementBuilder.OngoingReadingAndWith withIdMap =
                // 내 친구 수집
                matchUserWithId(userId, "me")
                .match(friendshipBetween(me, friendship(), friend))
                .with(collect(friend).as(friendList))
                // friendList에 담긴 노드들의 elementId(key), property.id(value) 매핑
                .with(friendList, ApocPatterns.idMapOf(friendList).as(idMap));

        // 2. 수집된 친구 목록 내에서 서로 간의 관계(Edge)를 APOC 알고리즘으로 체이닝 탐색
        Statement statement = ApocPatterns.chainFriendshipNetwork(withIdMap, friendList, friendList, path)
                // 쿼리 기준 path가 반환됨
                // nodesOfPath : [0](startNode)-[1](Friendship)-[2](terminatorNode)
                .with(nodesOfPath.as(pathNodes), idMap)
                .returning(
                        // idMap에서 nodes[0], nodes[2]의 property.id 조회, nodes[1]에서 intimacy 조회
                        friendAId.as("friendA_Id"),
                        friendBId.as("friendB_Id"),
                        intimacy.as("intimacy")
                )
                .build();

        String cypher = renderer.render(statement);

        // 파라미터 바인딩 , 쿼리 실행 및 결과 객체 매핑
        return neo4jClient.query(cypher)
                .bindAll(statement.getCatalog().getParameters())
                .fetchAs(NetworkFriendEdgeResult.class)
                .mappedBy((typeSystem, record) -> new NetworkFriendEdgeResult(
                        record.get("friendA_Id").asLong(),
                        record.get("friendB_Id").asLong(),
                        record.get("intimacy").asDouble(0.0)
                ))
                .all()
                .stream()
                .toList();
    }

    /**
     * targetIds 중 실제 친구인 노드만 필터링한 뒤 Edge List 반환
     * getFriendsNetwork와 동일한 로직을 수행하되 친구 리스트를 내부에서 찾는 게 아닌 외부에서 일부 친구 목록을 전달
     * 실제로 친구인지 검증만 수행
     */
    @Override
    public List<NetworkFriendEdgeResult> getVerifiedFriendsNetwork(Long userId, Collection<Long> targetIds) {

        Node me = user().named("me");
        Node friend = user().named("friend");

        SymbolicName verifiedFriendList = name("verifiedFriendList");
        SymbolicName idMap = name("idMap");
        SymbolicName path = name("path");
        SymbolicName pathNodes = name("pathNodes");


        Expression nodesOfPath = call("nodes").withArgs(path).asFunction();
        Expression friendANode = valueAt(pathNodes, 0);
        Expression middleFriendshipNode = valueAt(pathNodes, 1);
        Expression friendBNode = valueAt(pathNodes, 2);
        Expression friendAId = valueAt(idMap, call("elementId").withArgs(friendANode).asFunction());
        Expression friendBId = valueAt(idMap, call("elementId").withArgs(friendBNode).asFunction());
        Expression intimacy = coalesce(property(middleFriendshipNode, PROP_INTIMACY), literalOf(0.0));

        // targetIds 파라미터와 실제 친구 목록을 대조하여 교차하는 친구만 수집, idMap 생성
        StatementBuilder.OngoingReadingAndWith withIdMap = matchUserWithId(userId, "me")
                .match(friendshipBetween(me, friendship(), friend))
                // 친구 중에서 id가 targetIds에 속한 친구만 반환(실제로 친구인게 확인된 친구만 수집됨)
                .where(friend.property(PROP_ID).in(parameter("targetIds")))
                .with(collect(friend).as(verifiedFriendList))
                // idMap 생성
                .with(verifiedFriendList, ApocPatterns.idMapOf(verifiedFriendList).as(idMap));

        Statement statement = ApocPatterns.chainFriendshipNetwork(withIdMap, verifiedFriendList, verifiedFriendList, path)
                .with(nodesOfPath.as(pathNodes), idMap)
                .returning(
                        friendAId.as("friendA_Id"),
                        friendBId.as("friendB_Id"),
                        intimacy.as("intimacy")
                )
                .build();

        String cypher = renderer.render(statement);

        return neo4jClient.query(cypher)
                .bind(targetIds).to("targetIds")
                .fetchAs(NetworkFriendEdgeResult.class)
                .mappedBy((typeSystem, record) -> new NetworkFriendEdgeResult(
                        record.get("friendA_Id").asLong(),
                        record.get("friendB_Id").asLong(),
                        record.get("intimacy").asDouble(0.0)
                ))
                .all()
                .stream()
                .toList();
    }

    /**
     * 친밀도 상위 boundarySize 내에서 상위 coreSize 만큼을 출발점으로, boundarySize 전체를 도착점으로 Edge List 반환
     * getFriendsNetwork와 동일한 로직을 수행하되 startNodes, terminatorNodes 친구목록을 일치시키지 않고 startNodes를 친밀도 기준으로 축소
     * 친한 일부 친구(coreSize)에 대해서만 전체 boundarySize에 대해 친구관계를 탐색
     */
    @Override
    public List<NetworkFriendEdgeResult> getTopIntimateFriendsNetwork(Long userId, int boundarySize, int coreSize) {

        Node me = user().named("me");
        Node friend = user().named("friend");
        Node fsNode = friendship().named("fs");

        SymbolicName networkBoundary = name("networkBoundary");
        SymbolicName coreNodes = name("coreNodes");
        SymbolicName idMap = name("idMap");
        SymbolicName path = name("path");
        SymbolicName pathNodes = name("pathNodes");

        Expression nodesOfPath = call("nodes").withArgs(path).asFunction();
        Expression friendANode = valueAt(pathNodes, 0);
        Expression middleFriendshipNode = valueAt(pathNodes, 1);
        Expression friendBNode = valueAt(pathNodes, 2);
        Expression friendAId = valueAt(idMap, call("elementId").withArgs(friendANode).asFunction());
        Expression friendBId = valueAt(idMap, call("elementId").withArgs(friendBNode).asFunction());
        Expression intimacy = coalesce(property(middleFriendshipNode, PROP_INTIMACY), literalOf(0.0));

        // 전체 경계(boundary) 리스트에서 핵심 노드(core) 리스트를 슬라이싱하여 추출
        Expression topSlice = subList(networkBoundary, 0, coreSize);

        // 친밀도 내림차순 정렬 후 지정된 크기(boundarySize)만큼 친구 노드 제한 수집
        StatementBuilder.OngoingReadingAndWith withIdMap = matchUserWithId(userId, "me")
                .match(friendshipBetween(me, fsNode, friend))
                .with(friend, fsNode)
                .orderBy(fsNode.property(PROP_INTIMACY).descending())
                .limit(boundarySize)
                .with(collect(friend).as(networkBoundary))
                // 핵심 노드 집합(coreNodes)과 전체 탐색 경계(networkBoundary)에 대한 ID 매핑 테이블 준비
                .with(networkBoundary,
                        topSlice.as(coreNodes),
                        ApocPatterns.idMapOf(networkBoundary).as(idMap));

        // 핵심 노드들을 시작점으로 하여 전체 경계 내 노드들 사이의 모든 관계망 탐색
        Statement statement = ApocPatterns.chainFriendshipNetwork(withIdMap, coreNodes, networkBoundary, path)
                .with(nodesOfPath.as(pathNodes), idMap)
                .returning(
                        friendAId.as("friendA_Id"),
                        friendBId.as("friendB_Id"),
                        intimacy.as("intimacy")
                )
                .build();

        String cypher = renderer.render(statement);

        return neo4jClient.query(cypher)
                .bindAll(statement.getCatalog().getParameters())
                .fetchAs(NetworkFriendEdgeResult.class)
                .mappedBy((typeSystem, record) -> new NetworkFriendEdgeResult(
                        record.get("friendA_Id").asLong(),
                        record.get("friendB_Id").asLong(),
                        record.get("intimacy").asDouble(0.0)
                ))
                .all()
                .stream()
                .toList();
    }

    /**
     * 나의 관심도(interestScore) 상위 boundarySize 내에서 상위 coreSize 만큼을 출발점으로, boundarySize 전체를 도착점으로 Edge List 반환
     */
    @Override
    public List<NetworkFriendEdgeResult> getTopInterestFriendsNetwork(Long userId, int boundarySize, int coreSize) {

        Node me = user().named("me");
        Node friend = user().named("friend");
        Node fsNode = friendship().named("fs");

        // 💡 변경점 1: 나와 Friendship을 잇는 관계에 'myRel' 이라는 이름을 부여합니다.
        Relationship myRel = me.relationshipTo(fsNode, HAS_FRIENDSHIP).named("myRel");

        SymbolicName networkBoundary = name("networkBoundary");
        SymbolicName coreNodes = name("coreNodes");
        SymbolicName idMap = name("idMap");
        SymbolicName path = name("path");
        SymbolicName pathNodes = name("pathNodes");

        Expression nodesOfPath = call("nodes").withArgs(path).asFunction();
        Expression friendANode = valueAt(pathNodes, 0);
        Expression middleFriendshipNode = valueAt(pathNodes, 1);
        Expression friendBNode = valueAt(pathNodes, 2);
        Expression friendAId = valueAt(idMap, call("elementId").withArgs(friendANode).asFunction());
        Expression friendBId = valueAt(idMap, call("elementId").withArgs(friendBNode).asFunction());
        Expression intimacy = coalesce(property(middleFriendshipNode, PROP_INTIMACY), literalOf(0.0));

        Expression topSlice = subList(networkBoundary, 0, coreSize);

        // 💡 변경점 2: 매칭과 정렬 기준을 'myRel'의 'interestScore'로 교체합니다.
        StatementBuilder.OngoingReadingAndWith withIdMap = matchUserWithId(userId, "me")
                // 패턴: (me)-[myRel:HAS_FRIENDSHIP]->(fs)<-[:HAS_FRIENDSHIP]-(friend)
                .match(myRel.relationshipFrom(friend, HAS_FRIENDSHIP))
                .with(friend, myRel)
                // 친밀도(fsNode.intimacy) 대신 내 관심도(myRel.interestScore)로 정렬!
                .orderBy(myRel.property(PROP_INTEREST_SCORE).descending())
                .limit(boundarySize)
                .with(collect(friend).as(networkBoundary))
                .with(networkBoundary,
                        topSlice.as(coreNodes),
                        ApocPatterns.idMapOf(networkBoundary).as(idMap));

        // 이 아래의 APOC 탐색 로직은 기존과 100% 동일하게 완벽 작동합니다!
        Statement statement = ApocPatterns.chainFriendshipNetwork(withIdMap, coreNodes, networkBoundary, path)
                .with(nodesOfPath.as(pathNodes), idMap)
                .returning(
                        friendAId.as("friendA_Id"),
                        friendBId.as("friendB_Id"),
                        intimacy.as("intimacy")
                )
                .build();

        String cypher = renderer.render(statement);

        return neo4jClient.query(cypher)
                .bindAll(statement.getCatalog().getParameters())
                .fetchAs(NetworkFriendEdgeResult.class)
                .mappedBy((typeSystem, record) -> new NetworkFriendEdgeResult(
                        record.get("friendA_Id").asLong(),
                        record.get("friendB_Id").asLong(),
                        record.get("intimacy").asDouble(0.0)
                ))
                .all()
                .stream()
                .toList();
    }

    /**
     * 특정 2-Hop 유저(targetUser)와 나 사이에 겹치는 친구(oneHopFriend) 목록을 조회
     */
    @Override
    public List<NetworkOneHopsByTwoHopResult> getIntersectionOneHops(Long userId, Long targetId) {

        Node me = user().named("me");
        Node myFriend = user().named("myFriend");
        Node targetUser = user().named("targetUser");
        Node mutual = user().named("mutual");
        Node targetFs = friendship().named("targetFs");

        SymbolicName myFriends = name("myFriends");

        // 타겟 사용자의 프라이버시 설정인 isRoutable 속성을 검사하기 위해 관계 식별자 정의
        Relationship targetRel = targetUser.relationshipTo(targetFs, HAS_FRIENDSHIP).named("targetRel");

        // 현재 사용자의 모든 친구 노드를 수집
        StatementBuilder.OngoingReadingAndWith withMyFriends = matchUserWithId(userId, "me")
                .match(friendshipBetween(me, friendship(), myFriend))
                .with(collect(myFriend).as(myFriends));

        // 캐싱된 내 친구 목록과 타겟 사용자의 친구 목록을 교차 검증하여 공통 친구 도출
        Statement statement = withMyFriends
                .match(targetUser)
                .where(idEquals(targetUser, targetId))
                // 타겟 사용자로부터 시작하여 중간 Friendship 노드를 거쳐 친구(mutual)로 이어지는 경로 매칭
                .match(targetRel.relationshipFrom(mutual, HAS_FRIENDSHIP))
                /*
                   타겟 사용자가 해당 친구 관계에 대해 '친구의 친구'에게 노출을 허용(isRoutable)했는지 확인
                   노출이 허용된 타겟의 친구들 중 현재 사용자의 친구 목록(myFriends)에 포함된 교집합 노드만 추출
                */
                .where(isRoutable(targetRel)
                        .and(mutual.getRequiredSymbolicName().in(myFriends)))
                .returning(mutual.property(PROP_ID).as("friendId"))
                .build();

        String cypher = renderer.render(statement);

        return neo4jClient.query(cypher)
                .bindAll(statement.getCatalog().getParameters())
                .fetchAs(NetworkOneHopsByTwoHopResult.class)
                .mappedBy((typeSystem, record) -> new NetworkOneHopsByTwoHopResult(
                        record.get("friendId").asLong()
                ))
                .all()
                .stream()
                .toList();
    }

    @Override
    public List<MutualFriendEdgeResult> getIntersectionByOneHop(Long userId, Long targetId) {

        Node me = user().named("me");
        Node mutual = user().named("mutual");
        Node myFs = friendship().named("myFs");

        Node target = user().named("target");
        Node targetFs = friendship().named("targetFs");

        // 프라이버시(isRoutable) 검사를 위한 타겟 친구 관계 명시
        Relationship targetRel = target.relationshipTo(targetFs, HAS_FRIENDSHIP).named("targetRel");

        Statement statement = matchUserWithId(userId, "me")
                // 나와 교집합 친구 사이의 관계 매칭
                .match(friendshipBetween(me, myFs, mutual))

                // 타겟 유저 매칭
                .match(target).where(idEquals(target, targetId))

                // 타겟 유저와 교집합 친구 사이의 관계 매칭
                .match(targetRel.relationshipFrom(mutual, HAS_FRIENDSHIP))

                // 타겟 유저의 노출 허용 여부(isRoutable) 조건 검사
                .where(isRoutable(targetRel))

                // 딱 필요한 데이터(Edge 정보)만 리턴
                .returning(
                        target.property(PROP_ID).as("friendAId"),
                        mutual.property(PROP_ID).as("friendBId"),
                        targetFs.property(PROP_INTIMACY).as("intimacy")
                )
                .build();

        String cypher = renderer.render(statement);

        // Neo4jClient를 통한 쿼리 실행 및 매핑
        return neo4jClient.query(cypher)
                .bindAll(statement.getCatalog().getParameters())
                .fetchAs(MutualFriendEdgeResult.class)
                .mappedBy((typeSystem, record) -> new MutualFriendEdgeResult(
                        record.get("friendAId").asLong(),
                        record.get("friendBId").asLong(),
                        record.get("intimacy").asDouble()
                ))
                .all()
                .stream()
                .toList();
    }
}
