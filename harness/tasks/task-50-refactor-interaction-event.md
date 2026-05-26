# Task 50 — InteractionType mutual 플래그 추가 및 이벤트 구조 단순화

## Objective

현재 상호작용 이벤트가 일방(`UserInteractionEvent`)과 양방(`MutualInteractionEvent`)으로 클래스가 분리되어 있다.
`InteractionType`에 `mutual` 플래그를 추가하고, `MutualInteractionEvent`를 제거하여 이벤트 클래스를 하나로 통합한다.
점수 정책(`InteractionScorePolicy`)은 변경하지 않는다.

## Background

- `UserInteractionEvent(actorId, targetId, type)` — actor 쪽 interestScore만 증가
- `MutualInteractionEvent(userIdA, userIdB, type)` — 양쪽 interestScore 증가
- `BatchMutualInteractionEvent(participantIds, hostId, type)` — N명 전체 쌍 양방 증가

함께한 활동(FLAG_ENDED, FLAG_ENDED_ENCORE)은 두 참여자 모두 관심도가 올라야 한다.
이 "양방 여부"는 이벤트 클래스가 아닌 `InteractionType` 자체가 알고 있어야 한다.

## Scope

**In Scope**
- `InteractionType`에 `boolean mutual` 필드 추가
- `UserInteractionEvent` 필드명 중립화: `actorId/targetId` → `userA/userB`
- `MutualInteractionEvent` 삭제
- `FriendInteractionEventListener` 핸들러 통합 (`handleMutualInteraction` 제거, `handleUserInteraction`에 mutual 분기 추가)
- 기존 발행 지점(`BuzzInteractionEventListener`, `FlagDeletionEventListener`, `Trace` 등) 호환 확인

**Out of Scope**
- `InteractionScorePolicy` 수정 없음
- `BatchMutualInteractionEvent` 수정 없음
- intimacy 갱신 로직 변경 없음

## Edge Cases

- `UserInteractionEvent`를 발행하는 모든 지점에서 필드명 변경(`actorId → userA`) 반영 필요
- `mutual=true` 타입을 `UserInteractionEvent`로 발행하는 기존 코드가 없는지 확인 (현재는 `MutualInteractionEvent`로 분리되어 있으므로 누락 가능성 낮음)

---

## Result (task-50, 2026-05-27)

**브랜치:** `ai/refactor-interaction-event` → `main` 머지

### 발견 사항

- `MutualInteractionEvent`는 리스너만 존재하고 **발행 지점이 없는 데드코드** 상태였음
- `FLAG_ENDED`, `FLAG_ENDED_ENCORE`는 이미 `BatchMutualInteractionEvent`로만 발행되고 있었음

### 변경 내역

| 파일 | 내용 |
|---|---|
| `InteractionType.java` | `mutual` 필드 추가 (FLAG_ENDED, FLAG_ENDED_ENCORE = true) |
| `UserInteractionEvent.java` | 필드명 `actorId/targetId → userA/userB` 중립화 |
| `MutualInteractionEvent.java` | 삭제 |
| `FriendInteractionEventListener.java` | `handleMutualInteraction` 제거, `handleUserInteraction`에 `type.isMutual()` 분기 추가 |
| `FriendInteractionEventListenerTest.java` | `MutualInteractionEvent` 케이스 → `UserInteractionEvent(FLAG_ENDED)` 케이스로 교체, unilateral 1회 호출 검증 추가 |

`InteractionScorePolicy`, `BatchMutualInteractionEvent`, `FlagDeletionEventListener` 변경 없음.
