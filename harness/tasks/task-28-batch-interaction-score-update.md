# Task: 친구 관계 상호작용 점수 배치 업데이트 도입

## Background

* 현재 `FriendInteractionEventListener`는 `UserInteractionEvent`, `MutualInteractionEvent`를 개별적으로 처리하며, 이벤트 하나당 Neo4j `findById` + `save` 2번의 DB 왕복이 발생함.
* `FlagDeletionEventListener`는 Flag 종료 시 참가자 N명에 대해 O(N²)개의 `MutualInteractionEvent`를 발행하여 스레드 풀에 O(N²)번의 작업을 제출하고 동일한 횟수의 Neo4j 트랜잭션을 발생시킴.
* SDN의 `save()`는 `SET n = $allProps` 방식으로 노드의 **전체 프로퍼티를 덮어씀**. 이로 인해 `interestScore` 업데이트와 사용자의 alias/mute 변경이 동시에 발생할 경우 lost update가 발생할 수 있음.
* `@Version`(낙관적 락)은 단일 이벤트 처리를 전제로 "충돌이 드물다"는 가정 하에 도입됐으나, 배치로 많은 friendship을 한 번에 처리하는 환경에서는 이 가정이 성립하지 않음.
* 멀티 서버 배포 환경에서 인메모리 버퍼를 각 인스턴스가 독립적으로 들고 있으면 동시 write 문제가 발생하므로, 공유 버퍼로 Redis를 사용함.

## Objective

* Flag 종료 이벤트를 O(N²)개의 개별 이벤트 대신 하나의 `BatchMutualInteractionEvent`로 발행하여 스레드 풀 부하를 제거함.
* 모든 상호작용 이벤트(VISIT, BUZZ_SEND, BUZZ_REPLY, FLAG_ENDED 등)의 delta를 Redis 버퍼에 누적하고 주기적으로 일괄 처리하여 Neo4j 왕복 횟수를 최소화함.
* Friendship의 사용자 명시 업데이트(alias, mute, isRoutable)를 SDN `save()` 대신 필드별 `@Query` 메서드로 교체하여 lost update 문제를 근본적으로 제거함.
* 위 변경으로 `@Version`이 불필요해지므로 Friendship에서 제거함.

## Domain Change

[ ] 없음  [x] 있음
* `Friendship`에서 `@Version` 필드 제거
* `FriendshipRepository` 포트에 배치 write 메서드 및 필드별 update 메서드 추가
* `FlagDeletionEventListener`의 이벤트 발행 방식 변경 (`BatchMutualInteractionEvent` 신규 생성)

## Decision

### 이벤트 발행 변경
* `FlagDeletionEventListener.publishInteractionEvents()`가 발행하던 O(N²)개의 `MutualInteractionEvent`를 `BatchMutualInteractionEvent(List<Long> participantIds, Long hostId, InteractionType type)` **1개**로 대체함.
* VISIT, BUZZ_SEND, BUZZ_REPLY 등 나머지 이벤트는 기존 개별 이벤트 유지 (발행 자체의 오버헤드가 작으므로).

### Redis 버퍼 구조
* 이벤트 수신 시 Neo4j에 직접 쓰는 대신 Redis에 delta를 누적함.
* Key 구조: `interaction:delta:{friendshipId}:{userId}` → `HINCRBYFLOAT`으로 원자적 누적.
* 멀티 서버 환경에서 모든 인스턴스가 동일한 Redis 버퍼에 쓰므로 delta 유실 없음.

### 주기적 Flush (ShedLock)
* `@Scheduled(fixedDelay = 5000)` + `@SchedulerLock`(ShedLock)으로 **한 인스턴스만** flush 실행을 보장함.
* Flush 흐름:
    1. Redis 버퍼에서 `(friendshipId, userId, accumulatedDelta)` 목록 drain
    2. 유니크 friendshipId 목록으로 Neo4j MATCH 1번 (batch read)
    3. Java 도메인 로직: `friendship.adjustInterestScore(userId, delta)` → `recalculateIntimacy()` 자동 호출
    4. 계산된 최종값(interestScore, intimacy)을 파라미터로 Neo4j UNWIND 1번 (batch write)
* ShedLock이 단일 writer를 보장하므로 delta가 아닌 **final value**를 UNWIND에 넘겨도 lost update 없음.
* 도메인 로직(`adjustInterestScore`, `recalculateIntimacy`)이 Java에 완전히 유지됨.

### Friendship 업데이트 방식 변경
* SDN `save()`(`SET n = $allProps`) 제거 → 필드별 `@Query` 메서드로 교체.
    * `updateAlias(friendshipId, userId, alias)` → `SET rel.friendAlias = $alias`
    * `updateMuteStatus(friendshipId, userId, isMuted)` → `SET rel.isMuted = $isMuted`
    * `updateRoutableStatus(friendshipId, userId, isRoutable)` → `SET rel.isRoutable = $isRoutable`
* 각 쿼리가 해당 필드만 건드리므로 interestScore 배치 업데이트와 필드가 겹치지 않아 충돌 없음.
* `@Version` 낙관적 락도 함께 제거함.

### Out of Scope
* Flush 실패(Neo4j 장애 등) 시 Redis 버퍼 재투입 전략은 별도 태스크로 처리.
* Redis 버퍼 TTL 및 최대 적재량 제한은 별도 태스크로 처리.
