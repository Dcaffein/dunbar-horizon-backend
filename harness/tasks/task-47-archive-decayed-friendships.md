# Task-47: 친밀도 감쇄 우정 Neo4j → MySQL 이관

## 배경

`FriendshipDecayPolicy`에 의해 상호작용이 없으면 친밀도(intimacy)가 감쇄되고 `MIN_RAW_THRESHOLD`가 하한선으로 작동한다. 장기간 교류가 없는 관계가 Neo4j에 계속 누적되면 그래프 쿼리(네트워크 탐색, 친구 추천 등) 성능이 저하된다. 하한선에 도달한 휴면 관계를 Neo4j에서 제거하고 MySQL 아카이브 테이블로 이관한다.

## 목표

- 하한선(MIN_RAW_THRESHOLD)에 도달한 Friendship을 Neo4j에서 삭제한다
- 관계 이력을 MySQL `archived_friendships` 테이블에 보존한다
- 이관된 관계는 재친구 맺기로만 회복 가능하다
- Neo4j에서 삭제되면 기존 그래프 쿼리(친구 목록, 친밀도 탐색)에서 자연히 제외되며, 공통 친구를 통한 2-hop 추천에는 계속 노출될 수 있다 (의도된 동작)

---

## 수치 설계

### FriendRecognition 초기값 변경

| 항목 | 현재 | 변경 |
|------|------|------|
| `INITIAL_RAW_SCORE` | 5.5 | **21.4** |
| 초기 intimacy | 0.0 (interestScore가 0으로 시작) | **≈ 0.30** |

친구를 맺은 시점부터 의미 있는 친밀도(0.3)로 시작한다.
`FriendRecognition` 생성자에서 `interestScore = INITIAL_RAW_SCORE`로 초기화.

### FriendshipDecayPolicy 수치 변경

| 항목 | 현재 | 변경 |
|------|------|------|
| `GRACE_PERIOD_DAYS` | 7일 | **30일** |
| `ACTUAL_DECAY_DAYS` | 30일 | **90일** |
| `DECAY_FROM` | `INITIAL_RAW_SCORE` 참조 (5.5) | **21.4** (초기값과 분리) |

```java
// 예: 21.4점이 90일 뒤에 1.0점이 되려면 매일 약 3.3%씩 감소해야 함 (Rate ≒ 0.967)
// 하한선 1.0 → normalized ≈ 0.02 (intimacy 2% 미만 = 이관 대상)
private static final double DECAY_FROM = 21.4;
private static final double DECAY_REFERENCE_SCORE = 1.0;
private static final int ACTUAL_DECAY_DAYS = 90;
private static final int GRACE_PERIOD_DAYS = 30;
private static final double MIN_RAW_THRESHOLD = 1.0;
```

`DECAY_FROM`을 `FriendRecognition.INITIAL_RAW_SCORE`에서 분리하여 초기값과 감쇄 속도를 독립적으로 관리한다.

### 이관 타임라인 (교류 없을 때)

```
day 0        친구 맺음 (interestScore = 21.4, intimacy ≈ 0.30)
day 0~30     유예기간 — 감쇄 없음
day 30~120   감쇄 진행: score 21.4 → 1.0 (하한선 도달 시 0.0으로 설정, intimacy = 0.0)
day 120      이관 대상 확정 (약 4개월)
```

> 감쇄 쿼리는 score가 MIN_RAW_THRESHOLD(1.0) 아래로 내려가면 0.0으로 설정한다.  
> 이관 배치는 `intimacy = 0.0`인 Friendship을 대상으로 한다.

---

## FriendshipArchivePolicy (신규)

`FriendshipDecayPolicy`는 감쇄 속도를 결정하고, `FriendshipArchivePolicy`는 이관 기준을 결정한다.

```java
public double archiveThreshold() {
    return 0.0; // 감쇄 쿼리가 MIN_RAW_THRESHOLD 아래 score를 0.0으로 설정하므로, intimacy = 0.0이 이관 기준
}
```

하한선 도달 = 이관 후보. 별도 날짜 조건 없이 intimacy threshold만으로 트리거한다.

---

## 아카이브 스키마 (MySQL)

```sql
CREATE TABLE archived_friendships (
    id            VARCHAR(64) PRIMARY KEY,  -- Neo4j composite id (minId_maxId)
    user_a_id     BIGINT NOT NULL,
    user_b_id     BIGINT NOT NULL,
    peak_intimacy DOUBLE,                   -- 생애 최고 친밀도
    friended_at   DATE NOT NULL,
    archived_at   DATETIME NOT NULL
);
```

---

## 처리 흐름

```
FriendshipArchiveScheduler (야간 배치, ShedLock)
  └─ FriendshipArchivePolicy.archiveThreshold() 로 이관 기준 조회
  └─ Neo4j: interestScore ≤ 0.1인 Friendship 조회
  └─ MySQL: archived_friendships INSERT (배치)
  └─ Neo4j: 대상 Friendship 노드 DELETE
```

MySQL INSERT 완료 확인 후 Neo4j DELETE 진행 — 두 저장소가 단일 트랜잭션으로 묶이지 않으므로 순서로 유실 방지.

---

## 주요 변경 파일

| 파일 | 내용 |
|------|------|
| `FriendRecognition.java` | `INITIAL_RAW_SCORE` 5.5 → 21.4, 생성자 `interestScore = INITIAL_RAW_SCORE`로 초기화 |
| `FriendshipDecayPolicy.java` | `DECAY_FROM` 분리, `GRACE_PERIOD_DAYS` 7 → 30, `ACTUAL_DECAY_DAYS` 30 → 90 |
| `FriendshipArchivePolicy.java` | 신규 — 이관 임계값 정책 |
| `ArchivedFriendship.java` | MySQL JPA 엔티티 |
| `ArchivedFriendshipRepository.java` | JPA 포트 |
| `FriendshipRepository` | `findByInterestScoreBelow(threshold)` 추가 |
| `FriendshipArchiveService.java` | 이관 로직 (조회 → INSERT → DELETE) |
| `FriendshipArchiveScheduler.java` | ShedLock 기반 야간 배치 트리거 |

## 브랜치

`ai/feat-archive-decayed-friendships`

## 결과

- `FriendRecognition.INITIAL_RAW_SCORE` 5.5 → 21.4, 생성자 `interestScore = INITIAL_RAW_SCORE` 초기화 → 초기 intimacy ≈ 0.30
- `FriendshipDecayPolicy`: `DECAY_FROM = 21.4` 독립 상수 분리, `GRACE_PERIOD_DAYS` 7→30, `ACTUAL_DECAY_DAYS` 30→90, `MIN_RAW_THRESHOLD` 0.1→1.0
- `applyDecay` Cypher 버그 수정: `THEN 0.0` → `THEN $threshold` (하한선 보존), intimacy 재계산에 `normalize(raw) = raw / (raw + 50.0)` 적용
- `FriendshipArchiveCandidate` record, `FriendshipArchivePolicy`(threshold = normalize(1.0) ≈ 0.0196) 신규 생성
- `ArchivedFriendship` JPA 엔티티 + `archived_friendships` 테이블 스키마 (id, userAId, userBId, friendedAt, archivedAt)
- `FriendshipArchiveService`: JPA `@Transactional` MySQL INSERT 후 `@Neo4jTransactional` Neo4j DELETE 순서 보장
- `FriendshipArchiveScheduler`: 새벽 4시 ShedLock 기반 야간 배치
- `JpaConfig`에 `social.adapter.out.persistence.jpa` 패키지 추가
- `FriendshipDecayPolicyTest`, `FriendshipArchiveServiceTest` 단위 테스트 작성
