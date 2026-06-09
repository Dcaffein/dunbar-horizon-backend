# Task-59: 유저 간 최강 연결 경로 탐색 API

## Background

특정 유저와 나 사이의 소셜 그래프 상 경로를 찾아,
intimacy 기반 기하평균 점수가 가장 높은 경로를 반환하는 네트워크 분석 기능.

대상 유저는 직접 친구가 아니어도 되며, 전체 그래프에서 3홉 이내의 경로를 탐색한다.
"이 사람과 어떻게 연결돼 있지?"라는 명시적 탐색 용도이므로 수 초의 응답시간을 허용한다.

## Objective

- `GET /api/v1/networks/path?targetId={userId}` 엔드포인트 추가
- `apoc.path.expandConfig` 기반 전체 그래프 3홉 탐색
- 경로 스코어 = 경로 상 Friendship.intimacy 기하평균
- 복수 경로 중 최고 점수 경로 반환
- 3홉 이내 경로 없으면 빈 결과 반환

## Key Design Decisions

- **탐색 범위**: Dunbar 150명 제한 없음 — 전체 그래프
- **최대 홉**: 3홉 (HEX 패턴 구조상 관계 traversal 6단계 = `maxLevel: 6`)
- **스코어링**: intimacy 기하평균 `(i1 × i2 × ... × in)^(1/n)`
- **isRoutable**: 비대칭 적용
  - **내가 설정한 `isRoutable = false`** → 블랙리스트 제외. 내 분석 쿼리에서는 무시 (내 프라이버시 선택이지, 내 탐색을 막는 게 아님)
  - **타인이 설정한 `isRoutable = false`** → 해당 Friendship을 블랙리스트에 포함. 그 유저가 이 연결을 경유지로 쓰이는 걸 원하지 않는 것
  - 구현: `WHERE r.isRoutable = false AND u.id <> $myId`로 나를 제외한 타인의 non-routable Friendship만 사전 조회해 `blacklistNodes`에 전달
- **비활성 유저 필터**: `labelFilter: '+USER_REFERENCE|+Friendship'`으로 자동 처리
  - `SocialUser.deactivate()` 시 `USER_REFERENCE` 레이블이 제거되고 `INACTIVE_SOCIAL_USER`로 교체됨
  - `USER_REFERENCE`로만 탐색하면 비활성 유저는 애초에 집계되지 않음
- **응답 시간**: 수 초 허용 (명시적 분석 요청 기능, 로딩 UX 제공)
- **커넥션 보호**: 트랜잭션 타임아웃 설정으로 커넥션 풀 영향 차단

## expandConfig 파라미터

```cypher
-- 1단계: 타인이 설정한 non-routable Friendship 사전 조회
MATCH (u:USER_REFERENCE)-[r:HAS_FRIENDSHIP]->(f:Friendship)
WHERE r.isRoutable = false AND u.id <> $myId
WITH collect(DISTINCT f) AS blocked

-- 2단계: 경로 탐색
CALL apoc.path.expandConfig(me, {
  relationshipFilter: 'HAS_FRIENDSHIP',      -- 양방향 (HEX 구조상 잘못된 방향 경로는 자연 차단)
  labelFilter:        '+USER_REFERENCE|+Friendship',  -- 두 노드 타입만 탐색, 비활성 유저 자동 제외
  maxLevel:           6,                     -- 3홉 = 관계 6단계
  uniqueness:         'NODE_PATH',           -- 경로 내 중복 노드 방지 (NODE_GLOBAL은 최고 경로 탐색 불가)
  terminatorNodes:    [target],              -- target 도달 시 해당 브랜치 확장 중단
  blacklistNodes:     blocked,              -- 타인의 non-routable Friendship 제외
  bfs:                true                   -- BFS로 짧은 경로 우선 탐색
})
```

## isRoutable 처리

blacklistNodes 사전 수집 방식(전역 collect) 대신 APOC으로 경로를 먼저 탐색한 뒤 Cypher WHERE로 후처리 필터링한다.

```cypher
CALL apoc.path.expandConfig(...) YIELD path
WHERE NONE(
  r IN relationships(path)
  WHERE type(r) = 'HAS_FRIENDSHIP'
    AND r.isRoutable = false
    AND startNode(r) <> me
)
```

사전 수집 방식은 요청마다 그래프 전체를 스캔하는 고정 비용이 붙는다. 후처리 방식은 이미 탐색된 경로의 in-memory 데이터를 보는 거라 추가 DB hit이 없다.

## 경로 수 상한 및 타임아웃 — 프로파일링 필요

두 값 모두 실측 전 확정하지 않는다.

**경로 수 상한 (LIMIT N)**

BFS 특성상 짧은 경로가 먼저 yield된다. 2-hop 경로 수 = 공통 친구 수이며 C/D 그룹 기준 수십~100명 수준. 기하평균 특성상 2-hop 고친밀도 경로가 3-hop을 이기는 경우가 많아 2-hop을 전부 커버하는 선이면 충분하다. 시작값 **N = 200**, 프로파일링 후 조정.

**트랜잭션 타임아웃**

"경로 없음" 케이스가 worst case다. target에 도달하지 못하면 APOC이 maxLevel까지 도달 가능한 모든 노드를 탐색하고 나서야 반환한다. D그룹 유저의 3-hop 이웃 규모를 실측해야 timeout이 실제로 필요한지, 필요하다면 몇 초가 적당한지 알 수 있다. 시작값 **5초**, 프로파일링 후 조정.

**프로파일링 항목**
- C/D 그룹 유저 기준 target까지 3-hop 이내 경로 수 분포
- "경로 없음" 케이스 실행 시간 (D그룹 유저 기준 worst case)
- LIMIT N 변화에 따른 최고 점수 경로 결과 변동 여부

## Decision

미정. PLAN.md에서 구체화 예정.
