# Task-137: 친구 요청 id에 유일성 제약을 건다

> 취소 · 친구 요청을 MySQL로 옮길 때 두 사용자 사이의 요청 유일성을 DB 제약으로 보장한다.

## Background

친구 요청 id는 `FriendRequest.generateId`가 `작은id_큰id`로 만든다. A→B와 B→A가 같은 id가 되어, 한 쌍에 요청은 하나만 둔다는 규칙을 id로 표현한다.

이 규칙은 두 겹으로 지키도록 되어 있다.

- `FriendshipBroker.propose`가 양방향으로 기존 요청을 확인한다.
- `FriendRequestRequesterActionService.sendRequest`가 저장 중 `DataIntegrityViolationException`을 받으면 `DuplicateFriendRequestException`으로 바꾼다. `FriendRequestConcurrencyTest`도 "pairKey 제약으로 하나만 성공한다"를 검증한다.

그런데 이 예외를 낼 제약을 만드는 곳이 없다. Neo4j 마이그레이션(`V0001`~`V0003`)은 `FRIENDS_WITH`의 id 제약 하나만 만들고, 저장 쿼리(`FriendRequestNeo4jRepository.mergeFriendRequest`)는 `MERGE`가 아니라 `CREATE`다. 사전 확인과 저장 사이에 다른 요청이 끼어들면 같은 id의 요청 노드가 둘 생기고, 이후 `findById`가 결과를 둘 받아 수락이 실패한다.

운영 DB에 손으로 만든 제약이 남아 있는지는 확인하지 않았다(추측). 테스트가 통과한다면 타이밍 덕분일 수 있다.

## Objective

친구 요청 id의 유일성이 마이그레이션으로 보장되고, 동시에 들어온 두 요청 중 하나만 저장된다.

## Decision

### 제약은 마이그레이션으로 만든다

`FriendRequest` 노드의 `id`에 유일성 제약을 거는 새 마이그레이션을 추가한다. `IF NOT EXISTS`로 써서, 운영에 같은 제약이 이미 있어도 실패하지 않게 한다. 기존 마이그레이션 파일은 고치지 않는다.

### 도메인의 사전 확인은 유지한다

사전 확인은 친절한 오류와 대부분의 중복을 맡고, 제약은 그 사이로 끼어든 경합만 막는다. 둘 중 하나만 남기지 않는다.

## Out of Scope

- 친구 요청 저장 쿼리를 `MERGE`로 바꾸는 것
- 친구 요청 상태 모델

## Acceptance Criteria

- 새 마이그레이션이 `FriendRequest.id` 유일성 제약을 만들고, 이미 있는 환경에서도 실패하지 않는다.
- `FriendRequestConcurrencyTest`를 제약 없이 돌리면 실패할 수 있고, 제약을 둔 뒤에는 반복 실행해도 통과한다.
