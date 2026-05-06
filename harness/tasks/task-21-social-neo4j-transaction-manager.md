# Task 21: Social 도메인 트랜잭션 매니저 정합성 수정

## Objective

`social` 도메인 서비스들의 `@Transactional`이 `@Primary`인 JPA 트랜잭션 매니저를 사용하고 있어
Neo4j 다중 쓰기 작업의 원자성이 보장되지 않는 문제를 수정한다.
`@Neo4jTransactional` 메타 애노테이션을 도입하여 서비스 코드에서 트랜잭션 매니저에 대한 관심을 분리한다.

## Domain Change
[ ] 없음

---

## 배경

`@Transactional`에 `transactionManager`를 명시하지 않으면 `@Primary`인 JPA 트랜잭션이 열린다.
`@EnableNeo4jRepositories(transactionManagerRef = "neo4jTransactionManager")` 설정은 리포지토리 빈에만
적용되고 서비스 레이어 `@Transactional`에는 영향을 주지 않는다.

결과적으로 social 서비스들의 각 Neo4j 리포지토리 호출이 개별 트랜잭션으로 실행된다.
친구 요청 수락처럼 여러 Neo4j 쓰기가 필요한 작업에서 부분 실패 시 데이터 불일치가 발생할 수 있다.

---

## 알려진 제약

- `SocialUserSyncHelper.syncAndSave()`는 `userProfilePort`(JPA)와 `socialUserRepository`(Neo4j)를 함께 사용한다.
  `userProfilePort`는 JPA 트랜잭션 매니저를 직접 사용하므로 Neo4j 트랜잭션 설정과 무관하다.
- 호출자(`SocialUserService`)에서 `readOnly`를 제거하여 `syncAndSave()`가 같은 Neo4j tx에 참여하도록 한다.
  `readOnly`를 유지하면 `REQUIRES_NEW`가 필요해지므로 더 복잡해진다.
- `FriendInteractionEventListener`는 `@Async` + `AFTER_COMMIT`으로 실행되어 호출자 트랜잭션이 없으므로
  `REQUIRES_NEW`와 `REQUIRED`의 동작이 동일하다. `@Neo4jTransactional`(REQUIRED)으로 충분하다.
