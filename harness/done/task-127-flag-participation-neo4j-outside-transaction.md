# Task-127: 일반 참여의 친구 확인을 MySQL 트랜잭션 밖으로 뺀다

## Background

`FlagParticipationService.participateInFlag`는 메서드 전체가 JPA 트랜잭션이다. 그 안에서 `FlagParticipationManager.participate`가 hostId 조회, 친구 확인, 잠금 조회, 중복 검사, 참여자 수 조회를 차례로 하고, 서비스가 참여자를 저장한다.

친구 확인은 `FlagUserAdapter.areFriends` → `FriendshipQueryService`로 가고, 그쪽은 `@Neo4jTransactional(readOnly = true)`다. 트랜잭션 매니저가 달라 JPA 트랜잭션에 합류하지 않고 따로 열고 닫는다. 그래서 두 트랜잭션은 의미상 이미 독립이고 시간상으로만 겹친다.

비용은 MySQL 커넥션 점유다. hostId 조회에서 받은 커넥션을 Neo4j 왕복 동안 쥐고 있다. Flag 행 잠금은 친구 확인 뒤에 잡으므로 잠금 보유 시간에는 영향이 없다. 잠금 밖으로 Neo4j를 뺀 것은 task-27(`d8e5048`)이고, 이번 작업은 커넥션 점유 구간에서도 빼는 것이다.

## Objective

일반 참여가 hostId 조회, 친구 확인, 참여 확정 세 구간으로 나뉜다. 친구 확인 동안에는 MySQL 트랜잭션이 열려 있지 않다. 참여 확정 구간은 지금과 같은 잠금, 검사 순서, `READ_COMMITTED`를 유지한다.

## Decision

### 친구 여부 판단은 도메인에 남긴다

Neo4j 조회는 애플리케이션 서비스로 옮기지만 "호스트와 친구여야 참여할 수 있다"는 판정은 `FlagParticipationManager`가 한다. 서비스는 조회한 boolean을 넘긴다. 판정까지 서비스로 옮기면 참여 규칙이 두 계층에 흩어진다. `FlagParticipationManager`는 `FriendshipChecker`에 의존하지 않게 된다.

### 친구 확인은 잠금보다 앞이다

task-116에서 유지하기로 한 순서다. 친구가 아니면 잠금을 잡기 전에 거절한다.

### 구간을 나눠도 정합성은 그대로다

- `Flag.hostId`는 생성자에서만 대입된다. 따로 읽어도 낡지 않는다.
- 두 구간 사이에 Flag가 삭제되면 참여 확정의 잠금 조회가 `FlagNotFoundException`을 던진다.
- 친구 확인 직후 관계가 끊기는 경합은 지금도 있다. Neo4j가 이미 별도 트랜잭션이라 새로 생기는 틈이 아니다.

### 탈퇴는 이번에도 잠그지 않는다

task-115, task-116에서 정한 대로다. 탈퇴는 참여자 수를 줄이는 방향이라 정원 불변식을 깨지 않는다.

## Out of Scope

- 초대 참여(`participateByInvitation`)와 초대 수락 경로. 친구 확인이 없어 Neo4j를 거치지 않는다.
- `leaveFlag`의 트랜잭션과 잠금
- 같은 사용자가 탈퇴를 동시에 두 번 보낼 때 두 번째 삭제가 0행이 되는 경우의 동작
- `FriendshipQueryService`의 트랜잭션 설정

## Acceptance Criteria

- `areFriends`가 호출되는 시점에 JPA 트랜잭션이 활성 상태가 아니다. 이를 확인하는 테스트가 수정 전 동작에서 실패한다.
- 친구가 아니면 Flag 잠금 조회 없이 `FlagAuthorizationException`으로 거절된다.
- 존재하지 않는 Flag는 친구 확인 없이 `FlagNotFoundException`으로 거절된다.
- `FlagParticipationLockConcurrencyIntegrationTest`의 기존 시나리오가 모두 통과한다. 특히 일반 참여의 마감 경합과 정원 한 자리 경합.
- `leaveFlag`의 트랜잭션 동작은 바뀌지 않는다.
