# Task-83: Friendship 감쇄 버그 수정 및 코드 정비

## 배경

매일 새벽 3시 `FriendshipDecayScheduler`가 `applyDecay` Cypher 쿼리를 실행한다.
이 쿼리에 버그가 있어 특정 조건에서 활성 친구 관계의 `Friendship.intimacy`가 `0.0`으로 강제 설정된다.
그 결과 새벽 4시 `FriendshipArchiveScheduler`가 해당 관계를 이관 대상으로 판단하여 Neo4j에서 삭제한다.

`archived_friendship` 테이블에서 실제 피해가 확인되었다:
- `2_4` — `friended_at: 2025-11-29`, `archived_at: 2026-06-16 04:00 KST` (6개월 된 활성 관계 삭제)
- `4_11`, `4_7` — `archived_at: 2026-06-15 04:00 KST`
- `4_29` — `archived_at: 2026-06-16 04:00 KST`
- 최초 발생: 2026-05-28

---

## 버그 원인 분석

### `applyDecay` 쿼리 구조

```cypher
MATCH (u:UserReference)-[r:HAS_FRIENDSHIP]->(f:Friendship)
WHERE r.interestScore > $threshold
AND   r.lastInteractedAt <= $decayTime     -- 엣지 단위 필터
SET r.interestScore = ...
WITH f, collect(r.interestScore) as scores -- 필터 통과한 r만 수집
SET f.intimacy = sqrt(
    CASE WHEN size(scores) >= 2
    THEN ...
    ELSE 0.0 END                           -- 1개면 0.0으로 덮어씀 ← 버그
)
```

`Friendship` 노드에는 `HAS_FRIENDSHIP` 엣지가 정확히 2개(양쪽 유저 각각) 존재한다.
그런데 `collect(r.interestScore)`는 WHERE를 통과한 엣지만 수집하므로, 둘 중 하나만 조건을 충족하면
`scores` 크기가 1이 되고 실제 interestScore 수치와 무관하게 `f.intimacy = 0.0`이 된다.

**발동 시나리오:**

| 시나리오 | 원인 | 결과 |
|---------|------|------|
| A — 한쪽이 최근 교류 | `lastInteractedAt`이 최신 → WHERE 탈락 | scores 크기 1 |
| B — 한쪽이 이미 최솟값(1.0) | `1.0 > 1.0` false → WHERE 탈락 | scores 크기 1 |

**이관까지 연결되는 이유:**
```
archiveThreshold = normalize(1.0) = 1.0 / (1.0 + 50.0) ≈ 0.0196
f.intimacy(0.0) <= 0.0196  →  TRUE  →  MySQL 이관 후 Neo4j 노드 삭제
```

### 버그의 기원

`Friendship.recalculateIntimacy()`에 아래 방어 코드가 있다:
```java
if (this.recognitions.size() < 2) {
    this.intimacy = 0.0;
    return;
}
```
Java에서는 `this.recognitions`가 항상 2개이므로 실행될 수 없는 dead branch다.
Cypher 작성자가 이 패턴을 보고 그대로 번역했으나, Cypher의 `collect(r)`는 WHERE 필터 결과만 담으므로
"절대 발동하지 않는 방어 코드"가 "빈번히 실행되는 버그"가 됐다.

---

## 변경 사항

### 1. `applyDecay` 쿼리 수정

감쇄 조건과 이관 조건은 그대로 유지한다. 버그는 판정 로직이 아닌 collect 범위 문제다.

감쇄 적용 후 `WITH DISTINCT f`로 영향받은 Friendship을 추출하고, 전체 엣지를 재조회해서 intimacy를 재계산한다.
`ELSE 0.0` 분기도 제거한다 — 전체 엣지 재조회로 항상 2개가 보장되므로 불필요하다.

### 2. `Friendship.recalculateIntimacy()` 정비

버그의 기원이 된 dead branch(`size < 2 → intimacy = 0.0`)와
의미 없이 복잡도를 높이는 `calculateIntimacyPolicy()` static 메서드를 제거하고 `Math.sqrt` 인라인으로 단순화한다.

### 3. `FriendshipNeo4jRepository` 쿼리 정비

`Friendship.id`로 직접 접근하는 일부 @Query를 SDN6 상속 메서드(derived)로 대체한다.

- `findById` override 제거
- `findAllByIds` → `findAllById` (상속)
- `deleteAllByIdIn` → `deleteAllById` (상속)

UserReference 경유 탐색 쿼리 및 관계 프로퍼티 조건 쿼리는 @Query 유지
(SDN6 derived query는 Friendship 자신의 프로퍼티만 조건으로 사용 가능)

시그니처 변경에 따라 `FriendshipRepositoryAdapter`, `FriendshipRepository` 포트,
호출부(`InteractionScoreFlushService`, `FriendshipArchiveService`)도 함께 수정한다.

---

## 변경하지 않는 것

- 감쇄 조건 (`r.lastInteractedAt`, `r.interestScore > $threshold`) 및 수치
- 이관 조건 및 Archive 로직
- `InteractionScoreFlushService` 현행 구조
- Neo4j 스키마/constraint — task-84에서 별도 처리

---

## 브랜치

`ai/fix-friendship-decay-intimacy`
