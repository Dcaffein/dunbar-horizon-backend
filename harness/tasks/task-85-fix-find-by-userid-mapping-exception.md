# Task-85: findByUserId 쿼리 비활성 유저 미처리 수정

## 배경

`GET /api/v1/friends` 에서 `MappingException` 이 발생했다. 직접 삽입된 더미 데이터의
타입 불일치 문제(`DATE_TIME` vs `LOCAL_DATE_TIME`)는 별도로 처리되며, 이 Task는
`findByUserId` 쿼리 자체의 버그에 집중한다.

---

## 버그 원인

```cypher
MATCH (:UserReference {id: $userId})-[:HAS_FRIENDSHIP]->(f:Friendship)
MATCH (f)<-[all_r:HAS_FRIENDSHIP]-(all_u:UserReference)     -- ← 문제
RETURN f, collect(all_r), collect(all_u)
```

`SocialUser` 노드는 활성화 상태에 따라 동적 레이블이 바뀐다.

| 상태   | Neo4j 레이블               |
|--------|---------------------------|
| 활성   | `SocialUser:UserReference` |
| 비활성 | `SocialUser:InactiveSocialUser` |

두 번째 MATCH가 `(all_u:UserReference)` 로 필터하므로, 상대방이 비활성이면 해당 관계가
`collect(all_r)` 에서 누락되어 `recognitions.size() == 1` → `FriendshipInvalidException`
→ `MappingException` 이 발생한다.

### 단일 경로 MATCH로 합치면 더 나쁜 이유

```cypher
-- ❌ 비활성 친구가 있으면 f 자체가 결과에서 통째로 사라짐 (조용한 누락)
MATCH (:UserReference {id: $userId})-[:HAS_FRIENDSHIP]->(f:Friendship)
      <-[:HAS_FRIENDSHIP]-(:UserReference)
```

에러 대신 데이터 누락이 발생하므로 해결책이 아니다.

---

## 변경 사항

### `FriendshipNeo4jRepository.findByUserId` 쿼리 수정

비즈니스 요구: **비활성 친구가 있는 friendship은 목록에서 제외**해야 한다.

```cypher
-- 1단계: 친구 쪽이 UserReference인 경우만 f를 추출 (비활성 친구는 여기서 제외)
MATCH (:UserReference {id: $userId})-[:HAS_FRIENDSHIP]->(f:Friendship)
      <-[:HAS_FRIENDSHIP]-(:UserReference)

-- 2단계: 걸러진 f에 대해 레이블 제약 없이 양쪽 관계 수집 → recognitions 항상 2개 보장
MATCH (f)<-[all_r:HAS_FRIENDSHIP]-(all_u)
RETURN f, collect(all_r), collect(all_u)
```

**동작 원리:**
- 1단계 단일 경로에서 Cypher 경로 내 관계 유일성 규칙에 의해 익명 `(:UserReference)` 는
  반드시 `$userId` 본인이 아닌 상대방으로 매칭됨
- 상대방이 `InactiveSocialUser` 이면 1단계에서 행 자체가 없어져 f가 제외됨
- 2단계는 레이블 제약 없이 전체 관계를 수집하므로 양쪽 2개가 보장됨

### 변경 파일

- `FriendshipNeo4jRepository.java` — `findByUserId` `@Query` 수정

---

## 테스트 보완

`FriendshipNeo4jRepositoryTest` 에서 다음을 검토하고 필요한 케이스를 보완한다.

- `findByUserId` — 비활성 유저가 포함된 시나리오 추가
- `FriendshipNeo4jRepository` 의 다른 쿼리 메서드들도 비활성 유저 관련 동작에 문제가 없는지 함께 검토

---

## 변경하지 않는 것

- `FriendRecognition.user` 타입 (`UserReference` 인터페이스 유지 — 애그리거트 경계 보존)
- `Friendship.@PersistenceCreator` 검증 로직
- 기타 쿼리 메서드 (검토 후 이상 없으면 유지)

---

## 브랜치

`ai/fix-find-by-userid-mapping-exception`
