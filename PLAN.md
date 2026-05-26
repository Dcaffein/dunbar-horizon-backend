# PLAN: Task-47 친밀도 감쇄 우정 Neo4j → MySQL 이관

## 작업 목표

교류 없이 약 4개월이 지나 intimacy = 0.0으로 감쇄된 Friendship을 Neo4j에서 삭제하고, MySQL `archived_friendships` 테이블에 이력을 보존한다. 초기 친밀도 의미를 부여하기 위해 `FriendRecognition` 초기값 및 `FriendshipDecayPolicy` 수치도 함께 조정한다.

---

## 현황 분석

- `FriendRecognition.INITIAL_RAW_SCORE = 5.5`, 생성자 `interestScore = 0.0` → 초기 intimacy = 0.0
- `FriendshipDecayPolicy`: `GRACE_PERIOD_DAYS=7`, `ACTUAL_DECAY_DAYS=30`, `MIN_RAW_THRESHOLD=0.1`, `INITIAL_RAW` = `FriendRecognition.INITIAL_RAW_SCORE` (결합)
- `applyDecay` Cypher: score * rate < threshold → 0.0으로 설정 (threshold에 고정하지 않음)
  → 이관 기준은 `intimacy = 0.0` (score가 0으로 귀결된 Friendship)
- `JpaConfig.basePackages`에 `social` 패키지 없음 → `ArchivedFriendship` JPA 엔티티를 넣기 위해 추가 필요
- ShedLock Redis 프로바이더 이미 존재 (`InteractionScoreFlushScheduler` 사용 중)

---

## 변경 파일 목록

### 수정

| 파일 | 변경 내용 |
|------|-----------|
| `FriendRecognition.java` | `INITIAL_RAW_SCORE` 5.5→21.4, 생성자 `interestScore = INITIAL_RAW_SCORE` |
| `FriendshipDecayPolicy.java` | `DECAY_FROM = 21.4` 상수 추가 및 `INITIAL_RAW` 참조 제거, `getDecayRate()`에서 `DECAY_FROM` 사용, `GRACE_PERIOD_DAYS` 7→30, `ACTUAL_DECAY_DAYS` 30→90, `MIN_RAW_THRESHOLD` 0.1→1.0 |
| `FriendshipNeo4jRepository.java` | `applyDecay` Cypher: `THEN 0.0` → `THEN $threshold` (하한선 보존), intimacy 재계산 시 normalized 공식 적용 (`scores[i] / (scores[i] + 50.0)`), `deleteAllByIdIn` @Query 추가 |
| `FriendshipRepository.java` (port) | `findArchiveCandidates(double threshold)`, `deleteAllByIds(Collection<String> ids)` 추가 |
| `FriendshipRepositoryAdapter.java` | Neo4jClient 주입, 두 신규 메서드 구현 (`findArchiveCandidates`는 Neo4jClient 사용) |
| `JpaConfig.java` | `basePackages`에 `com.example.DunbarHorizon.social.adapter.out.persistence.jpa` 추가 |

### 신규 생성

| 파일 | 내용 |
|------|------|
| `social/domain/friend/FriendshipArchiveCandidate.java` | record(id, userAId, userBId, friendedAt) — Neo4j 쿼리 반환 DTO |
| `social/domain/friend/FriendshipArchivePolicy.java` | `archiveThreshold()` → 0.0 반환 |
| `social/adapter/out/persistence/jpa/ArchivedFriendship.java` | JPA 엔티티 |
| `social/adapter/out/persistence/jpa/ArchivedFriendshipJpaRepository.java` | JpaRepository |
| `social/domain/friend/repository/ArchivedFriendshipRepository.java` | 도메인 포트 |
| `social/adapter/out/ArchivedFriendshipRepositoryAdapter.java` | 어댑터 |
| `social/application/service/FriendshipArchiveService.java` | 이관 로직 (find+save: JPA @Transactional, delete: @Neo4jTransactional) |
| `social/adapter/in/scheduler/FriendshipArchiveScheduler.java` | @Scheduled + @SchedulerLock |

---

## 구현 방향

### 수치 변경 요약

```
INITIAL_RAW_SCORE  : 5.5 → 21.4  (초기 intimacy 0.0 → ≈0.30)
DECAY_FROM         : 신규 상수 21.4 (INITIAL_RAW_SCORE와 분리)
GRACE_PERIOD_DAYS  : 7 → 30
ACTUAL_DECAY_DAYS  : 30 → 90
MIN_RAW_THRESHOLD  : 0.1 → 1.0
```

### 이관 흐름 (FriendshipArchiveService)

MySQL INSERT 완료 후 Neo4j DELETE — 두 저장소가 별개 트랜잭션이므로 순서로 유실 방지.

```
archiveFriendships() [@Transactional JPA]
  1. findArchiveCandidates(0.0) → Neo4j에서 intimacy=0.0인 Friendship 목록 조회
  2. ArchivedFriendship 리스트 생성
  3. archivedFriendshipRepository.saveAll(...)
  4. 이관된 friendshipIds 반환

deleteArchivedFromNeo4j(ids) [@Neo4jTransactional]
  5. friendshipRepository.deleteAllByIds(ids)
```

스케줄러가 두 메서드를 순서대로 호출.

### applyDecay Cypher 수정

기존 `THEN 0.0`을 `THEN $threshold`로 교체하여 interestScore가 MIN_RAW_THRESHOLD(1.0) 아래로 내려가지 않도록 함.
intimacy 재계산도 normalized 공식으로 수정: `normalize(raw) = raw / (raw + CONVERGENCE_K=50.0)`.

```cypher
-- 수정 전
WHEN r.interestScore * $rate < $threshold THEN 0.0

-- 수정 후
WHEN r.interestScore * $rate < $threshold THEN $threshold

-- intimacy 재계산 수정 전 (raw 직접 사용 → 버그)
SET f.intimacy = sqrt(scores[0] * scores[1])

-- 수정 후 (normalize 적용)
SET f.intimacy = sqrt((scores[0]/(scores[0]+50.0)) * (scores[1]/(scores[1]+50.0)))
```

이 수정 후 floor 상태(양쪽 모두 1.0)의 intimacy = sqrt(normalize(1.0)²) = 1.0/51.0 ≈ 0.0196.

### findArchiveCandidates Cypher (Neo4jClient)

```cypher
MATCH (u:UserReference)-[:HAS_FRIENDSHIP]->(f:Friendship)<-[:HAS_FRIENDSHIP]-(v:UserReference)
WHERE f.intimacy <= $threshold AND u.id < v.id
RETURN f.id AS id, u.id AS userAId, v.id AS userBId, f.createdAt AS friendedAt
```

`u.id < v.id` 조건으로 양방향 중복 제거.

### ArchivedFriendship 스키마

```sql
id           VARCHAR(64) PRIMARY KEY  -- Neo4j composite id (minId_maxId)
user_a_id    BIGINT NOT NULL
user_b_id    BIGINT NOT NULL
friended_at  DATE
archived_at  DATETIME NOT NULL
```

`peak_intimacy` 제외 — 감쇄 후 항상 floor(≈0.02)이므로 의미 없음.

### FriendshipArchivePolicy.archiveThreshold()

```java
// normalize(MIN_RAW_THRESHOLD) = normalize(1.0) = 1.0 / (1.0 + 50.0) = 1/51 ≈ 0.0196
return FriendRecognition.normalize(decayPolicy.getMinThreshold());
```

decay Cypher 수정 후, floor 상태의 f.intimacy = normalize(1.0) ≈ 0.0196.

### 스케줄러

```java
@Scheduled(cron = "0 0 4 * * *")  // 새벽 4시 (FriendshipDecayScheduler 새벽 3시 이후)
@SchedulerLock(name = "friendshipArchive", lockAtMostFor = "PT30M", lockAtLeastFor = "PT1M")
```

---

## 예상 사이드 이펙트

- 기존에 `interestScore = 0.0`으로 생성된 Friendship(task-47 이전 데이터)은 이미 `intimacy = 0.0`이므로, 첫 배치 실행 시 일괄 이관 대상에 포함됨. 의도된 동작으로 수용.
- `JpaConfig.basePackages`에 `social.adapter.out.persistence.jpa` 추가 시, 해당 패키지의 JPA 엔티티·리포지토리만 스캔됨 (Neo4j 레포와 충돌 없음).

---

## 테스트 전략

**단위 테스트**

`FriendshipDecayPolicyTest` (MockitoExtension):
- `getDecayRate_사용값_검증` — DECAY_FROM=21.4, ACTUAL_DECAY_DAYS=90 기준 rate 계산

`FriendshipArchiveServiceTest` (MockitoExtension):
- `archiveFriendships_후보없으면_저장안함`
- `archiveFriendships_후보있으면_MySQL저장후_IDs반환`
- `deleteArchivedFromNeo4j_IDs전달_검증`
