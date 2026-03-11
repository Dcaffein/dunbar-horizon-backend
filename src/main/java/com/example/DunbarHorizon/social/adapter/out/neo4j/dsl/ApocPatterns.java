package com.example.DunbarHorizon.social.adapter.out.neo4j.dsl;

import org.neo4j.cypherdsl.core.*;

import static com.example.DunbarHorizon.social.adapter.out.neo4j.dsl.SocialNetworkProperties.PROP_ID;
import static com.example.DunbarHorizon.social.domain.friend.constant.FriendConstants.HAS_FRIENDSHIP;

public final class ApocPatterns {

    /**
     * <pre>
     * startNodes가 가지는 terminaotorNodes와의 친구 관계 (terminator 간 친구관계는 조회되지 않음)
     * startNode의 친구경로(2 * HAS_FRIENDSHIP)를 타고 넘어간 노드가 terminaotr Map에 존재하는 지 확인
     *      해당 경로는 탐색 시 재방문되지 않음 (RELATIONSHIP_GLOBAL)
     * DFS로 startNode들에 하나씩 적용됨 (BFS를 적용하면 startNode에 속하는 terminator를 조회하는 게 불가능)
     * </pre>
     *
     * <p><b>Cypher:</b>
     * <pre>
     * CALL apoc.path.expandConfig(startNodes, {
     *     startNodes: startNodes,
     *     terminatorNodes: terminatorNodes,
     *     relationshipFilter: "HAS_FRIENDSHIP",
     *     minLevel: 2,
     *     maxLevel: 2,
     *     uniqueness: "RELATIONSHIP_GLOBAL",
     *     bfs: false
     * }) YIELD path
     * </pre>
     *
     * @param builder          WITH 절까지 완성된 DSL 빌더
     * @param startNodes       탐색 시작 유저 목록
     * @param terminatorNodes  탐색 종료 유저 목록
     * @param pathVar          YIELD 받을 path 변수
     * @return YIELD 이후 이어서 체인 가능한 빌더 상태
     */
    public static StatementBuilder.OngoingInQueryCallWithReturnFields chainFriendshipNetwork(
            StatementBuilder.OngoingReadingAndWith builder,
            SymbolicName startNodes,
            SymbolicName terminatorNodes,
            SymbolicName pathVar) {

        MapExpression config = Cypher.mapOf(
                "startNodes", startNodes,
                "terminatorNodes", terminatorNodes,
                "relationshipFilter", Cypher.literalOf(HAS_FRIENDSHIP),
                "minLevel", Cypher.literalOf(2),
                "maxLevel", Cypher.literalOf(2),
                "uniqueness", Cypher.literalOf("RELATIONSHIP_GLOBAL"),
                "bfs", Cypher.literalFalse()
        );

        return builder.call("apoc.path.expandConfig")
                      .withArgs(startNodes, config)
                      .yield(pathVar);
    }

    /**
     *  <pre>타겟의 친구들 중 terminator 인 친구들의 개수 반환
     * 타겟의 친구들(2 * HAS_FRIENDSHIP)을 조회해서 terminator Map에 존재하는 지 확인</pre>
     *
     * <p><b>Cypher:</b>
     * <pre>
     * CALL apoc.path.expandConfig(startNodes, {
     *     startNode: target,
     *     terminatorNodes: terminatorNodes,
     *     relationshipFilter: "HAS_FRIENDSHIP",
     *     minLevel: 2,
     *     maxLevel: 2,
     *     uniqueness: "NODE_GLOBAL",
     *     bfs: true
     * }) YIELD path
     * </pre>
     *
     * @param target            terminatorNodes 중 몇개의 친구를 가지고 있는 지 확인할 대상
     * @param terminatorNodes   기준점이 될 친구 목록
     * @param me                startNodes들을 조회한 기준점
     * @param anchor            terminaotrNodes들을 조회한 기준점
     * @param resultAlias       YIELD 받을 path 변수
     */
    public static Statement countMutualFriendsSubquery(
            SymbolicName target,
            SymbolicName terminatorNodes,
            SymbolicName me,
            SymbolicName anchor,
            SymbolicName resultAlias) {

        MapExpression config = Cypher.mapOf(
                "relationshipFilter", Cypher.literalOf(HAS_FRIENDSHIP),
                "terminatorNodes", terminatorNodes,
                "deniedNodes", Cypher.listOf(me, anchor),
                "minLevel", Cypher.literalOf(2),
                "maxLevel", Cypher.literalOf(2),
                "uniqueness", Cypher.literalOf("NODE_GLOBAL")
        );

        SymbolicName pathVar = Cypher.name("path");

        return Cypher.with(target, terminatorNodes, me, anchor)
                .call("apoc.path.expandConfig")
                .withArgs(target, config)
                .yield(pathVar)
                .returning(Cypher.count(pathVar).as(resultAlias))
                .build();
    }

    /**
     * {@code apoc.map.fromPairs([f IN nodeListVar | [elementId(f), f.id]])} 표현식을 반환
     * nodeList의 element인 node f에 대해서
     *  f의 db 내부용 id인 element(id)를 key로
     *  f의 property인 .id를 value로 삼는 Map을 만듦
     * <p>APOC 탐색 후 경로 노드의 ID를 DB 재조회 없이 룩업하기 위한 메모리 맵 생성
     *
     * @param nodeListVar 노드 목록을 부르는 이름
     * @return {@code apoc.map.fromPairs(...)} 함수 표현식
     */
    public static Expression idMapOf(SymbolicName nodeListVar) {
        SymbolicName f = Cypher.name("f");
        return Cypher.call("apoc.map.fromPairs")
                .withArgs(
                    Cypher.listWith(f).in(nodeListVar)
                        .returning(Cypher.listOf(
                            Cypher.call("elementId").withArgs(f).asFunction(),
                            Cypher.property(f, PROP_ID)
                        ))
                ).asFunction();
    }

    private ApocPatterns() {}
}
