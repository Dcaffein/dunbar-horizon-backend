# Task 67: SocialExpansionRepositoryAdapter Cypher DSL → raw string 전환

## 배경

task-66에서 `SocialNetworkRepositoryAdapter`의 Cypher DSL을 raw string으로 전환했다.
`SocialExpansionRepositoryAdapter`도 동일한 문제를 가지고 있었다.

- DSL로 쿼리 1개를 표현하는 데 Java 코드 ~110줄 소요
- `Renderer`, `Configuration`, `StatementBuilder` 등 DSL 의존성
- `excludeMyFriends` 조건 분기를 Java if문으로 처리 → Cypher 파라미터로 대체 가능

## 변경 내용

- DSL 110줄 → `ANCHOR_EXPANSION_QUERY` raw string 단일 상수로 교체
- `excludeMyFriends` 분기: `AND (NOT $excludeMyFriends OR NOT target IN myFriends)` Cypher 파라미터로 처리
- `Renderer`, `Configuration`, `StatementBuilder`, Cypher DSL import 전체 제거
- `getRelatedNetworkByAnchor` / `getRecommendedNetworkByAnchor` → 공통 `executeQuery()` private helper 위임
- Neo4j 5.x CALL 서브쿼리 문법 사용: `CALL (target, myFriends, me, anchor) { ... }`
- `#{TOKEN}` → static replace 패턴으로 레이블/프로퍼티 상수 주입

## 쿼리 블록 구성

```
[1] me, anchor 확인 + me의 1-hop 친구 목록 수집 (anchor 제외)
[2] anchor가 소유하고 me가 속한 라벨 수집 (공유 맥락 기준)
[3] anchor의 2-hop 후보 탐색 — isRoutable 필터 + excludeMyFriends 조건 + 친밀도 정렬
[4] CALL subquery: 공통 지인 수 산출
[5] list comprehension: 공유 라벨 수 산출
[6] threshold 필터 + RETURN + LIMIT
```
