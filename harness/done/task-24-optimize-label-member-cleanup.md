# Task 24: 친구 삭제 시 라벨 멤버 정리 쿼리 최적화

## Objective
`LabelMemberShipEventListener`가 친구 삭제 이벤트를 처리할 때 owner의 **모든** 라벨을 로드한 뒤 Java에서 필터링하는 비효율을 제거한다.
`ownerId + memberId` 기준으로 DB 쿼리 자체를 좁혀 실제로 해당 멤버가 속한 라벨만 로드·저장하도록 개선한다.

## Background
- 현재 `removeFriendFromLabels(ownerId, memberIdToRemove)`는 `findAllByOwner_Id(ownerId)`로 모든 라벨을 로드한 뒤 각각 `removeMember`를 시도하고 `saveAll`로 전부 저장한다.
- 라벨이 많을수록 불필요한 노드 로드·불필요한 write가 선형으로 증가한다.
- 삭제된 친구가 실제로 속한 라벨은 전체 라벨 중 소수일 가능성이 높다.
- 별개 문제로, `findMemberIdsByOwnerAndLabelIds` 쿼리가 관계(`OWNS_LABEL`)가 아닌 `label.ownerId` 프로퍼티를 직접 참조하고 있어, 실제 Neo4j 스키마와 일치하는지 검증이 필요하다.

## Domain Change
[ ] 없음  [x] 있음 (포트 인터페이스에 신규 메서드 추가)

## Decision
- `LabelNeo4jRepository`에 Cypher 쿼리 메서드 추가:
  ```cypher
  MATCH (owner:UserReference {id: $ownerId})-[:OWNS_LABEL]->(label:Label)
        -[:ATTACHED_TO]->(member:UserReference {id: $memberId})
  RETURN label
  ```
- `LabelRepository` 포트 및 `LabelRepositoryAdapter`에 위임 메서드 추가.
- `LabelMemberShipEventListener`에서 기존 `findAllByOwner_Id` 대신 신규 메서드 사용.
- `findMemberIdsByOwnerAndLabelIds`의 `label.ownerId` 참조 방식도 위와 동일하게 `OWNS_LABEL` 관계 기반으로 통일 여부를 PLAN.md에서 결정한다.

---

## Result

**브랜치:** `ai/refactor-optimize-label-member-cleanup`
**커밋:** `0e35287`

### 변경 내용

#### 버그 수정
- `LabelNeo4jRepository.findMemberIdsByOwnerAndLabelIds`: `label.ownerId` 프로퍼티 참조 → `OWNS_LABEL` 관계 기반으로 수정
  - `Label` 노드에는 `ownerId` 프로퍼티가 존재하지 않아 항상 빈 Set을 반환하는 버그였음

#### 쿼리 최적화
- `LabelNeo4jRepository`에 `findLabelsByOwnerAndMember(ownerId, memberId)` 추가
  ```cypher
  MATCH (owner:UserReference {id: $ownerId})-[:OWNS_LABEL]->(label:Label)
        -[:ATTACHED_TO]->(member:UserReference {id: $memberId})
  RETURN label
  ```
- `LabelRepository` 포트에 동일 메서드 추가
- `LabelRepositoryAdapter`에 위임 메서드 추가

#### 리스너 개선 (`LabelMemberShipEventListener`)
- `findAllByOwner_Id` (전체 로드) → `findLabelsByOwnerAndMember` (대상 멤버가 속한 라벨만 로드)
- `SocialUserRepository` 의존성 제거 — `label.getMembers()` 컬렉션에서 직접 대상 노드 조회
- `saveAll` 조건부 실행 — 대상 라벨이 없으면 write 없이 종료

### 테스트 결과
- `LabelMemberShipEventListenerTest`: 2/2 PASSED (유닛)
- `LabelNeo4jRepositoryTest`: 6/6 PASSED (통합, Testcontainers Neo4j)
