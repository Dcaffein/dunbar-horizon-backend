# Task-153: social의 활성 사용자 판단을 저장소로 모으고 동시성 장치를 코드에 드러낸다

## Background

Friendship과 FriendRequest의 JPA 저장소는 조회 쿼리가 모두 네이티브였다. `FriendshipJpaRepository`에 4개(`findBetween`, `findByUserId`, `countBetween`, `deleteBetween`), `FriendRequestJpaRepository`에 4개(`findBetween`, `countBetween`, `findAllByReceiverIdAndStatus`, `findAllByRequesterId`)가 있었다.

네이티브인 이유는 하나였다. 모든 쿼리에 양쪽 사용자가 활성인지 거르는 조건(`ACTIVE_PAIR`)이 붙는데, 이 조건이 account 모듈의 `users` 테이블을 본다. 같은 관심사가 application에도 퍼져 있었다. 요청 서비스가 프로필을 조회해 상대가 활성인지 확인하고, 목록 서비스가 도메인 모델과 프로필을 메모리에서 조합했다. 다음 작업인 Label도 멤버를 활성 사용자로 걸러야 해서, 이 방식이면 같은 조건이 더 늘어난다.

## Objective

social의 도메인과 application은 사용자 활성 여부를 모르고, 저장소 계층이 활성 사용자와 관련된 것만 돌려준다. 명령 경로의 JPA 저장소는 기본 메서드와 파생 쿼리만 쓰고, 화면용 조회는 도메인 모델을 거치지 않는 조회 전용 쿼리로 읽는다. 활성 사용자 조건의 동작과 각 조회의 결과는 지금과 같다.

## Decision

- 쓰기 경로(애그리거트를 읽고 바꾸고 저장)는 JPA, 화면용 조회와 집합 단위 갱신은 JDBC로 나눈다.
- 명령 경로: adapter가 애그리거트를 읽기 전에 두 사용자가 활성인지 확인을 조합한다. 수단은 `users`를 읽기 전용으로 매핑하고 `@SQLRestriction("status = 'ACTIVE'")`을 건 `ActiveUser`(id만, `adapter/out/persistence/jpa`)다. 요청 생성도 저장 전에 adapter가 확인하고, 비활성이면 사용자 없음으로 거부한다. JPA 저장소 쿼리에는 활성 조건을 두지 않는다.
- 조회 경로: 친구 목록·상세와 요청 목록은 조회 전용 포트(`FriendViewRepository`, `FriendRequestViewRepository`)와 JDBC 구현이 `users`와 조인해 활성 상대만, 표시할 프로필까지 붙여 돌려준다. 점수 정규화·친밀도는 행을 `Friendship`으로 복원해 도메인 메서드로 계산한다(공식을 SQL에 다시 쓰지 않는다). 요청자에게 HIDDEN을 PENDING으로 보이는 규칙은 `FriendRequestStatus.visibleTo` 한 곳에 둔다.
- 프로필 조회(`UserProfileRepository`)는 조회 경로라 JDBC(`UserProfileJdbcRepository`)로 두고, 돌려주는 DTO는 `UserProfile`로 부른다. 활성 확인(`ActiveUser`)과 프로필 조회는 쓰는 주체와 경로가 달라 나눈다.
- 사용자별 친구 목록은 OR 한 쿼리로 합치지 않고 low·high 두 갈래를 `UNION ALL`로 읽어 각각 PK와 `idx_friendships_high_low`를 타게 한다.
- 친구 끊기는 라벨 정리와의 잠금 순서 때문에 즉시 실행되는 DELETE(task-152)를 유지한다. 파생 `deleteBy…`는 커밋 때 DELETE를 보내므로 쓰지 않는다.
- 기각한 대안
  - `users`를 읽기 전용 엔티티로 매핑하고 JPQL로 조건을 건다. 네이티브가 JPQL로 바뀔 뿐 조건이 메서드마다 반복되고 기본 메서드를 쓸 수 없다.
  - Friendship이 상대 사용자를 연관(`@ManyToOne`)으로 들고 파생 쿼리에 상태 조건을 건다. 조건이 메서드 이름마다 반복되고 기본 메서드에는 붙지 않는다. 사용자 상태는 account가 소유하며 Friendship의 규칙이 쓰지 않는다.
  - 도메인 엔티티에 `@SQLRestriction`으로 활성 조건을 건다. 한 번 구현해 썼다가 걷어 냈다. 조회 조건이 도메인 클래스에 붙고, 조회가 도메인 모델과 application의 프로필 조합에 기대는 구조를 그대로 둔다.
  - application이 경로마다 활성 여부를 확인한다. 경로마다 확인을 달아야 하고 하나라도 빠지면 결과가 갈린다(task-150이 저장소 조건으로 모은 이유).
- Label 작업보다 먼저 한다.
- 동시성 장치는 코드에서 읽히게 한다. 결과(409와 롤백)는 바꾸지 않는다.
  - 읽은 뒤 지워진 요청·관계: adapter가 삭제·상태 저장·설정 저장을 즉시 반영하고(`flushOrConflict`, `saveSettings`), 영향 행이 0건이면 `FriendRequestConflictException`·`FriendshipConflictException`을 던진다. 숨김·해제와 설정 변경은 서비스가 `saveStatus`·`saveSettings`로 명시적으로 저장한다. 에러 코드는 `CONCURRENT_MODIFICATION` 그대로다.
  - 중복 생성: adapter가 위반한 제약을 이름으로 구분한다. 친구 요청은 `PRIMARY`(같은 방향)와 `uk_friend_requests_pair`(방향과 무관한 쌍), 친구 관계는 `PRIMARY`(같은 쌍). 그 밖의 무결성 위반은 도메인 예외로 바꾸지 않는다.
  - 생명주기 테스트 이름은 "경합 → 막는 장치 → 결과"로 쓰고, 경합마다 테스트를 둔다.
- 트랜잭션 매니저는 하나뿐이라 `@Transactional`에 이름을 지정하지 않는다.

## Out of Scope

- 관계망 분석 조회(`SocialNetworkMysqlRepository`, `SocialExpansionMysqlRepository`, `SocialConnectionPathMysqlRepository`). 분석용 SQL이라 JDBC로 둔다.
- Label 저장과 서비스(`LabelMysqlRepository`, `MySqlLabelService`). 다음 Label 작업에서 `ActiveUser`와 조회 전용 쿼리로 같은 구조를 따른다.
- 점수 갱신·잠금 조회·ID 조회 같은 Friendship의 JDBC 경로(`FriendshipJdbcRepository`). `FriendshipConditions`의 SQL 조건을 계속 쓴다.
- 프로필 조회 API의 서비스(`AccountSocialUserQueryService`)와 그 이름.
- 휴면 사용자를 social에서 숨길지, 휴면 중 감쇠를 멈출지 같은 활성 상태 정책. 이 작업은 현재 정책을 그대로 옮긴다.
- 입력 포트(UseCase) 정리. task-154에서 다룬다.

## Acceptance Criteria

- `FriendshipJpaRepository`와 `FriendRequestJpaRepository`에 `nativeQuery = true`가 없다.
- `Friendship`·`FriendRequest`에 활성 사용자 조건이 없고, social application 서비스에 활성 여부 확인이 없다.
- 상대가 비활성이면 단건 조회는 없는 것으로, 목록에서는 보이지 않고, 요청 생성은 사용자 없음으로 끝난다. 이를 확인하는 테스트가 통과한다.
- 읽은 뒤 지워짐과 중복 생성의 판정이 adapter 코드에 이름 있는 예외와 제약 이름으로 드러나고, 경합마다 이름이 장치를 말하는 테스트가 있다.
- Friendship·FriendRequest 저장 어댑터 테스트와 생명주기·동시성 테스트가 통과한다.
