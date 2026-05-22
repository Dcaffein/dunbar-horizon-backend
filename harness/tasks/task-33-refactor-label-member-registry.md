# Task: LabelMemberRegistry 리팩터링

## Background

Label Member 등록 코드 분석에서 세 가지 개선 포인트가 도출됐다.

**문제 1: addNewMember의 UserReference 조회 위치 불일치 + contains() 신뢰성**

`addMemberToLabel`은 서비스에서 UserReference를 조회해 레지스트리로 넘기는 반면,
`replaceLabelMembers`는 레지스트리 내부에서 UserReference를 조회한다.
일관성이 없고, `addNewMember` 내부의 중복 체크 `label.getMembers().contains(potentialNewMember)`가
`UserReference`의 `equals/hashCode` 구현에 의존해 신뢰하기 어렵다.

**문제 2: Label.updateMembers() diff 방식의 효과 없음**

```java
this.members.removeIf(existing -> !newMembers.contains(existing));
this.members.addAll(newMembers);
```

Spring Data Neo4j의 `save()`는 `@Relationship` 컬렉션을 기존 관계 전부 삭제 후 재생성하는 방식으로
처리하므로, Java 레벨 diff가 DB 레벨 성능 개선으로 이어지지 않는다.
`clear + addAll`로 교체해 의도를 명확히 한다.

**비변경 항목: removeMemberFromLabel idempotent**

REST DELETE는 멱등성을 보장하는 것이 관례이므로, 없는 멤버 삭제 시 조용히 성공하는
현재 동작을 그대로 유지한다.

## Objective

- `LabelMemberRegistry.addNewMember()` 시그니처를 `Long newMemberId`로 변경하고 내부에서 UserReference를 조회한다.
- 중복 체크를 `contains()` → stream ID 비교로 교체한다.
- `LabelService.addMemberToLabel()`에서 UserReference 조회 제거 및 ID 전달로 변경한다.
- `Label.updateMembers()` 구현을 `clear + addAll`로 단순화한다.

## Domain Change

[ ] 없음  [x] 있음

- `LabelMemberRegistry` — `addNewMember(Label, UserReference)` → `addNewMember(Label, Long newMemberId)`
- `LabelService` — `addMemberToLabel()`에서 `socialUserRepository.findById(newMemberId)` 제거
- `Label` — `updateMembers()` 구현 단순화

## Decision

### LabelMemberRegistry.addNewMember()

```java
// 변경 전
public void addNewMember(Label label, UserReference potentialNewMember) {
    if (!friendshipRepository.existsFriendshipBetween(label.getOwner().getId(), potentialNewMember.getId())) { ... }
    if (label.getMembers().contains(potentialNewMember)) { ... }   // equals/hashCode 의존
    label.addNewMember(potentialNewMember);
}

// 변경 후
public void addNewMember(Label label, Long newMemberId) {
    if (!friendshipRepository.existsFriendshipBetween(label.getOwner().getId(), newMemberId)) { ... }
    if (label.getMembers().stream().anyMatch(m -> m.getId().equals(newMemberId))) { ... }  // ID 직접 비교
    UserReference newMember = socialUserRepository.findById(newMemberId)
            .orElseThrow(() -> new UserReferenceNotFoundException(newMemberId));
    label.addNewMember(newMember);
}
```

### LabelService.addMemberToLabel()

```java
// 변경 전
UserReference newMember = socialUserRepository.findById(newMemberId)
        .orElseThrow(() -> new UserReferenceNotFoundException(newMemberId));
labelMemberRegistry.addNewMember(label, newMember);

// 변경 후
labelMemberRegistry.addNewMember(label, newMemberId);
```

### Label.updateMembers()

```java
// 변경 전
void updateMembers(Set<UserReference> newMembers) {
    if (newMembers == null || newMembers.isEmpty()) {
        this.members.clear();
        return;
    }
    this.members.removeIf(existing -> !newMembers.contains(existing));
    this.members.addAll(newMembers);
}

// 변경 후
void updateMembers(Set<UserReference> newMembers) {
    this.members.clear();
    if (newMembers != null) {
        this.members.addAll(newMembers);
    }
}
```

## Checklist

- [ ] `LabelMemberRegistry.addNewMember()` 시그니처 변경 및 내부 UserReference 조회 + stream ID 비교
- [ ] `LabelService.addMemberToLabel()`에서 UserReference 조회 제거, ID 직접 전달
- [ ] `Label.updateMembers()` 단순화

## Result

- 브랜치:
- 커밋:
- 변경 파일:
