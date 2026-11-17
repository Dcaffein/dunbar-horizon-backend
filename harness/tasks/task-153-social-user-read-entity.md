# Task-153: social 조회용 사용자 엔티티로 네이티브 쿼리를 걷어 낸다

## Background

Friendship과 FriendRequest의 JPA 저장소는 조회 쿼리가 모두 네이티브다. `FriendshipJpaRepository`에 4개(`findBetween`, `findByUserId`, `countBetween`, `deleteBetween`), `FriendRequestJpaRepository`에 4개(`findBetween`, `countBetween`, `findAllByReceiverIdAndStatus`, `findAllByRequesterId`)가 있다.

네이티브인 이유는 하나다. 모든 쿼리에 양쪽 사용자가 활성인지 거르는 조건(`FriendshipConditions.ACTIVE_PAIR`, `FriendRequestConditions.ACTIVE_PAIR`)이 붙는데, 이 조건이 account 모듈의 `users` 테이블을 본다. `users`에 매핑된 JPA 엔티티는 `account/domain/User` 하나뿐이라, JPQL로 쓰려면 social 어댑터가 account의 엔티티를 참조해야 한다. 그래서 조건을 SQL 문자열로 두고 네이티브 쿼리를 썼다.

활성 조건이 없으면 이 쿼리들은 JPQL이나 파생 쿼리로 쓸 수 있다. 다음 작업인 Label도 멤버를 활성 사용자로 걸러야 해서, 지금 방식이면 네이티브 쿼리가 더 늘어난다.

네이티브 조회도 엔티티로 받아 영속성 컨텍스트에 올라가므로, 쓰기 경로(`persist`, `remove`의 영향 행 수 확인, 더티 체킹)는 이미 JPA가 처리한다. 바뀌는 것은 읽기 SQL뿐이다.

## Objective

social 저장 어댑터에 `users`를 읽기 전용으로 매핑한 엔티티가 생기고, Friendship과 FriendRequest의 JPA 저장소 쿼리가 이 엔티티를 쓰는 JPQL이나 파생 쿼리로 바뀐다. 활성 사용자 조건의 동작과 각 쿼리의 결과는 지금과 같다. 이후 Label은 처음부터 이 엔티티로 활성 조건을 건다.

## Decision

- 활성 조건은 social 어댑터에 둔 읽기 전용 엔티티로 건다. 이 엔티티는 도메인이 아니라 `adapter/out/persistence` 아래에 두고, `users`의 필요한 열만 매핑한다. 어댑터 계층이라 Hibernate 전용 어노테이션(예: `@Immutable`)을 써도 된다. JPA 엔티티 스캔 대상(`JpaConfig`의 base package)에 들어가는 위치여야 한다.
- 기각한 대안
  - account의 `User` 엔티티를 social 쿼리에서 직접 참조한다. social이 account의 모델에 묶인다.
  - 도메인 엔티티에 Hibernate `@SQLRestriction`으로 활성 조건을 건다. 모든 조회에 자동으로 붙지만 도메인 클래스에 Hibernate 전용 어노테이션과 SQL이 들어가, "도메인에는 `jakarta.persistence` 표준 어노테이션만 둔다"(task-151)와 어긋난다.
  - 활성 사용자를 메모리에서 거른다. task-151에서 SQL 조건으로 옮긴 것을 되돌리는 일이다.
- Label 작업보다 먼저 한다.
- 쓰기 경로와 동시성 판정(중복 INSERT의 409, DELETE·UPDATE 0건의 409)은 바꾸지 않는다.

## Out of Scope

- 관계망 분석 조회(`SocialNetworkMysqlRepository`, `SocialExpansionMysqlRepository`, `SocialConnectionPathMysqlRepository`). 분석용 SQL이라 JDBC로 둔다.
- Label 저장(`LabelMysqlRepository`). 다음 Label 작업에서 이 엔티티를 쓴다.
- 점수 갱신 같은 Friendship의 JDBC 경로(`FriendshipJdbcRepository`).
- `UserProfileRepository` 구현(`SocialUserMysqlRepository`)의 JDBC 조회.
- 입력 포트(UseCase) 정리. task-154에서 다룬다.

## Acceptance Criteria

- `FriendshipJpaRepository`와 `FriendRequestJpaRepository`에 `nativeQuery = true`가 없다.
- 상대가 비활성이면 단건 조회는 없는 것으로, 목록에서는 보이지 않는다. 이 동작을 확인하는 기존 테스트가 그대로 통과한다.
- 도메인 클래스(`Friendship`, `FriendRequest` 등)에 Hibernate 전용 어노테이션이 없다.
- Friendship·FriendRequest 저장 어댑터 테스트와 동시성 테스트가 통과한다.
