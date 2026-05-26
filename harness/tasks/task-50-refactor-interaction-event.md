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
