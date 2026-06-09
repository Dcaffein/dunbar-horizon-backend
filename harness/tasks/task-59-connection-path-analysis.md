# Task-59: 유저 간 연결 중개인 탐색 API

## Background

특정 유저의 프로필을 방문했을 때 "이 사람과 나는 어떻게 연결되어 있지?"를 보여주는 기능.
직접 친구가 아닌 유저를 대상으로 주로 사용되며, 공통 친구(중개인)를 통한 연결 맥락을 제공해 프로필 방문 동기를 높인다.

연결 강도는 경로 상 Friendship.intimacy 기하평균으로 측정한다.

## Objective

- `GET /api/v1/networks/path?targetId={userId}` 엔드포인트 추가
- 1-hop (직접 친구) 여부 확인
- 2-hop: 나와 target 사이의 공통 친구(중개인) 목록을 연결 강도 순으로 반환
- 연결 없으면 빈 결과 반환

## Key Design Decisions

### 탐색 방식: 고정 홉 Cypher MATCH

`me`와 `target` 양쪽 endpoint가 모두 바인딩되므로 Neo4j 쿼리 플래너가 양방향 탐색으로 최적화할 수 있다.
APOC `expandConfig`(단방향 flood-fill)와 달리 "경로 없음" 케이스에서도 빠르게 빈 결과를 반환한다.

| 항목                | APOC expandConfig      | 고정 홉 Cypher MATCH     |
|---------------------|------------------------|--------------------------|
| target 활용         | 탐색 후 필터           | 양방향 탐색 가능         |
| 2-hop 탐색 공간     | O(N²)                  | O(N)                     |
| "경로 없음" 비용    | N² 전체 탐색 후 반환   | 빠르게 빈 결과 반환      |
| APOC 플러그인       | 필수                   | 불필요                   |

### 홉별 쿼리 구조

```cypher
-- 1-hop: 직접 친구 여부 확인
MATCH (me:USER_REFERENCE {id: $myId})-[:HAS_FRIENDSHIP]->(f:Friendship)<-[:HAS_FRIENDSHIP]-(target:USER_REFERENCE {id: $targetId})
RETURN f.intimacy AS score

-- 2-hop: 공통 친구(중개인) 목록 조회
MATCH (me:USER_REFERENCE {id: $myId})
  -[:HAS_FRIENDSHIP]->(f1:Friendship)<-[:HAS_FRIENDSHIP]-(mid:USER_REFERENCE)
  -[:HAS_FRIENDSHIP]->(f2:Friendship)<-[:HAS_FRIENDSHIP]-(target:USER_REFERENCE {id: $targetId})
WHERE NONE(
  r IN [f1, f2]
  WHERE r.isRoutable = false AND startNode(r) <> me
)
WITH mid, f1.intimacy AS i1, f2.intimacy AS i2,
     sqrt(f1.intimacy * f2.intimacy) AS score
ORDER BY score DESC
RETURN mid, i1, i2, score
```

1-hop 확인 후 결과에 따라 2-hop 쿼리를 실행한다.
직접 친구가 아닌 경우가 주 사용 케이스이므로 대부분의 요청에서 2-hop 쿼리가 실행된다.

### 응답 형태

분석 기능이므로 단일 최적 경로가 아닌 **중개인 목록 전체**를 내려준다.
유저가 "A를 통해 연결될 수도 있고 B를 통해 연결될 수도 있구나"를 파악할 수 있어야 한다.

```json
{
  "direct": false,
  "intermediaries": [
    { "userId": "...", "name": "...", "score": 87.4 },
    { "userId": "...", "name": "...", "score": 72.1 }
  ]
}
```

### isRoutable 처리 (비대칭)

- **내가 설정한 `isRoutable = false`** → 내 탐색에서는 무시 (내 프라이버시 선택이지 탐색을 막는 게 아님)
- **타인이 설정한 `isRoutable = false`** → 해당 Friendship을 경유지에서 제외
- 구현: `WHERE NONE(r IN [f1, f2] WHERE r.isRoutable = false AND startNode(r) <> me)` 후처리

### 비활성 유저 필터

`USER_REFERENCE` 레이블로만 탐색하므로 자동 처리.
`SocialUser.deactivate()` 시 레이블이 `INACTIVE_SOCIAL_USER`로 교체되어 MATCH 패턴에 걸리지 않음.

### 3-hop 미포함

3-hop은 프라이버시 리스크(경유 노드 당사자가 인지 불가)와 낮은 실용성(친구의 친구의 친구를 중개인으로 활용하는 케이스가 드묾)으로 제외.
2-hop 공통 친구가 없으면 빈 결과 반환.
