# Task 50 — InteractionType mutual 플래그 추가 및 Friendship 양방향 점수 갱신 메서드 분리

## Objective

`InteractionType`에 `mutual` 플래그를 추가하고, `Friendship`에 양방향 점수 갱신 메서드를 분리한다.
"함께한 상호작용"(FLAG_ENDED 등)은 `Friendship.adjustMutualInterestScore(delta)` 한 번으로
양쪽 `FriendRecognition`을 동시에 올리고, `recalculateIntimacy()`를 단 한 번만 호출한다.
"일방적 상호작용"(VISIT 등)은 기존 `adjustInterestScore(userId, delta)`를 그대로 사용한다.

## Background

현재 mutual 상호작용(FLAG_ENDED)은 `BatchMutualInteractionEvent` 핸들러에서
`deltaPort.accumulate(friendshipId, userId, delta)`를 양쪽 userId로 각각 두 번 호출한다.
flush 시점에 `friendship.adjustInterestScore`도 두 번 호출되고, `recalculateIntimacy()`도 두 번 계산된다.
두 번째 계산만 정확하며 첫 번째는 한 쪽만 반영된 중간 상태다.

또한 mutual 여부를 Friendship이 아닌 리스너(응용 계층)가 판단하고 있어 도메인 책임이 잘못 배치되어 있다.

## Scope

**In Scope**
- `InteractionType`에 `boolean mutual` 필드 추가
- `UserInteractionEvent` 필드명 중립화: `actorId/targetId` → `userA/userB`
- `MutualInteractionEvent` 삭제 (발행 지점 없는 데드코드)
- `Friendship.adjustMutualInterestScore(double delta)` 추가
  - 두 `FriendRecognition` 모두 delta 적용 후 `recalculateIntimacy()` 1회 호출
- `InteractionScoreDeltaPort`에 `accumulateMutual(String friendshipId, double delta)` 추가
- `InteractionScoreRedisAdapter` — mutual용 별도 키 적재 및 drainAll 구조 변경
- `InteractionScoreFlushService` — mutual/unilateral 분기 후 적절한 Friendship 메서드 호출
- `FriendInteractionEventListener` — `type.isMutual()`에 따라 port 메서드 분기

**Out of Scope**
- `InteractionScorePolicy` 점수값 변경 없음
- `BatchMutualInteractionEvent` 구조 변경 없음 (발행 방식은 유지, 리스너 처리만 변경)
- intimacy 계산 정책 변경 없음

## Edge Cases

- 같은 Friendship에 unilateral과 mutual이 동시에 쌓일 수 있다 (방문 + 플래그 동시 발생).
- `BatchMutualInteractionEvent`: N명 참여자 각 쌍이 모두 mutual 처리 대상이다.
