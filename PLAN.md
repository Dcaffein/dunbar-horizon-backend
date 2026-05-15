# PLAN: Task-24 — 친구 삭제 시 라벨 멤버 정리 쿼리 최적화

## [도메인 수정 승인 요청]

`LabelRepository` 포트 인터페이스에 신규 메서드 추가:
```java
List<Label> findLabelsByOwnerAndMember(Long ownerId, Long memberId);
```

---

## 작업 목표

`LabelMemberShipEventListener.removeFriendFromLabels()`가 owner의 모든 라벨을 로드한 뒤 Java에서 필터링하는 비효율 제거.
`ownerId + memberId` 기준으로 DB 쿼리 자체를 좁혀 해당 멤버가 속한 라벨만 로드·저장한다.
아울러 `findMemberIdsByOwnerAndLabelIds`의 `label.ownerId` 프로퍼티 참조 버그를 함께 수정한다.

---

## 현황 분석

### 문제 1: `removeFriendFromLabels`의 과잉 로드·저장

`LabelMemberShipEventListener` (32-43줄):
- `labelRepository.findAllByOwner_Id(ownerId)` → owner의 **모든** 라벨 로드
- `socialUserRepository.findById(memberIdToRemove)` → 멤버 노드 별도 로드
- 각 라벨에 `removeMember` 시도 (멤버 미소속 라벨 포함)
- `saveAll(labels)` → 변경 없는 라벨 포함해 **전부** 저장

라벨 수가 많을수록 불필요한 노드 로드·write가 선형 증가.

### 문제 2: `findMemberIdsByOwnerAndLabelIds`의 잘못된 프로퍼티 참조 (버그)

`LabelNeo4jRepository` (26-28줄):
```cypher
WHERE label.ownerId = $ownerId  -- Label 노드에 ownerId 프로퍼티가 존재하지 않음!
```
`Label` 엔티티의 `owner` 필드는 `@Relationship(OWNS_LABEL)`으로 저장되어 Label 노드 자체에는 `ownerId` 프로퍼티가 없다.
이 쿼리는 항상 빈 Set을 반환하는 버그다.

---

## 변경 파일 목록

| 파일 | 변경 내용 |
|------|----------|
| `social/adapter/out/neo4j/springData/LabelNeo4jRepository.java` | `findLabelsByOwnerAndMember` 신규 추가 + `findMemberIdsByOwnerAndLabelIds` 쿼리 수정 |
| `social/domain/label/repository/LabelRepository.java` | `findLabelsByOwnerAndMember` 포트 메서드 추가 |
| `social/adapter/out/LabelRepositoryAdapter.java` | `findLabelsByOwnerAndMember` 위임 메서드 추가 |
| `social/application/eventListener/LabelMemberShipEventListener.java` | `findAllByOwner_Id` → `findLabelsByOwnerAndMember`로 교체, `SocialUserRepository` 의존성 제거 |

---

## 구현 방향

### 1. 신규 Cypher 쿼리 (`LabelNeo4jRepository`)

```cypher
MATCH (owner:UserReference {id: $ownerId})-[:OWNS_LABEL]->(label:Label)
      -[:ATTACHED_TO]->(member:UserReference {id: $memberId})
RETURN label
```
→ `List<Label> findLabelsByOwnerAndMember(@Param("ownerId") Long ownerId, @Param("memberId") Long memberId)`

### 2. `findMemberIdsByOwnerAndLabelIds` 쿼리 수정

기존:
```cypher
MATCH (label:Label)-[:ATTACHED_TO]->(member:UserReference)
WHERE label.ownerId = $ownerId AND label.id IN $labelIds
RETURN DISTINCT member.id
```
수정:
```cypher
MATCH (owner:UserReference {id: $ownerId})-[:OWNS_LABEL]->(label:Label)
      -[:ATTACHED_TO]->(member:UserReference)
WHERE label.id IN $labelIds
RETURN DISTINCT member.id
```

### 3. `LabelMemberShipEventListener` 개선

```java
private void removeFriendFromLabels(Long ownerId, Long memberIdToRemove) {
    List<Label> labels = labelRepository.findLabelsByOwnerAndMember(ownerId, memberIdToRemove);
    for (Label label : labels) {
        label.getMembers().stream()
            .filter(m -> m.getId().equals(memberIdToRemove))
            .findFirst()
            .ifPresent(label::removeMember);
    }
    if (!labels.isEmpty()) {
        labelRepository.saveAll(labels);
    }
}
```

**변경 포인트:**
- `socialUserRepository.findById()` 호출 제거 — SDN이 로드한 `label.getMembers()` 컬렉션에서 대상 객체를 직접 찾아 remove하므로 별도 조회 불필요
- 멤버가 속한 라벨 없는 경우 `saveAll` 자체를 skip
- `SocialUserRepository` 필드·의존성 제거

---

## 예상 사이드 이펙트

- `findMemberIdsByOwnerAndLabelIds` 버그 수정으로 `LabelService.getMemberIdsByLabels()`가 이제 올바른 멤버 ID를 반환함 — 기존보다 결과가 달라지지만 이는 버그 수정이므로 의도된 변경
- `LabelMemberShipEventListener`에서 `SocialUserRepository` 의존성 제거 — 테스트 mocking 대상 감소

---

## 테스트 전략

기본 프로토콜(`TESTING-PROTOCOL.md`) 준수. 아래 시나리오를 통합 테스트로 검증:

1. 멤버가 특정 라벨에 속한 경우 → 해당 라벨에서만 멤버 제거, 다른 라벨 불변
2. 멤버가 어떤 라벨에도 속하지 않은 경우 → DB 변경 없음(saveAll 미호출)
3. `findMemberIdsByOwnerAndLabelIds` → 수정된 쿼리가 올바른 멤버 ID를 반환하는지 확인
