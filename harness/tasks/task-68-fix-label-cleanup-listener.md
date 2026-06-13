# Task 68: LabelMemberShipEventListener 트랜잭션·비동기 구조 수정

## Objective

`LabelMemberShipEventListener.handleFriendShipDeleted`의 세 가지 구조적 결함을 수정한다.

## Background

`FriendshipCommandService.brokeUpWith`가 `FriendShipDeletedEvent`를 발행하면
`LabelMemberShipEventListener`가 라벨 멤버를 정리한다.
현재 이 리스너는 아래 세 가지 문제를 동시에 가진다.

### 문제 1 — 잘못된 트랜잭션 매니저 (버그)

```java
@Transactional   // transactionManager 미지정 → @Primary인 JPA(MySQL) 사용
public void handleFriendShipDeleted(...) {
    labelRepository.findLabelsByOwnerAndMember(...)  // Neo4j 쿼리
    labelRepository.saveAll(...)                      // Neo4j 쿼리
}
```

`LabelRepository`는 Neo4j를 사용하지만, `@Transactional`은 `@Primary`로 등록된
JPA 트랜잭션 매니저(`transactionManager`)를 선택한다.
Neo4j 작업이 트랜잭션 보호 없이 실행되는 상태다.

### 문제 2 — 잘못된 이벤트 리스너 타입 (데이터 정합성)

```java
@EventListener          // ← 트랜잭션 커밋 전에 실행
@Transactional
public void handleFriendShipDeleted(...) { ... }
```

`@EventListener`는 발행 시점(커밋 전)에 동기 실행된다.
friendship 삭제 트랜잭션이 이후 롤백되더라도 라벨 멤버는 이미 제거된 상태가 된다.
`SocialNetworkCacheEvictListener`는 동일 이벤트에 대해 올바르게
`@TransactionalEventListener(phase = AFTER_COMMIT)`을 사용하고 있다.

### 문제 3 — 동기 실행으로 인한 응답 지연

라벨 정리는 friendship 삭제의 부수 효과이며, HTTP 응답에 포함될 필요가 없다.
현재는 모든 라벨 쿼리·저장이 완료된 후에야 `brokeUpWith` API가 응답한다.

## Domain Change

[ ] 없음 [x] 있음 — 없음 (인터페이스 변경 없음, 리스너 어노테이션·설정만 수정)

## Decision

```java
@Async
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
@Neo4jTransactional
public void handleFriendShipDeleted(FriendShipDeletedEvent event) { ... }
```

| 변경 | 이유 |
|------|------|
| `@EventListener` → `@TransactionalEventListener(AFTER_COMMIT)` | friendship 삭제가 커밋된 후에만 라벨 정리 실행 |
| `@Transactional` → `@Neo4jTransactional` | Neo4j 작업에 올바른 트랜잭션 매니저 적용 |
| `@Async` 추가 | 라벨 정리를 응답 경로 밖으로 분리 |

`@Async`가 동작하려면 `AsyncConfig`(`@EnableAsync`)가 이미 설정되어 있어야 한다.
설정 여부를 확인하고 없으면 `global/config`에 추가한다.

## 수정 대상

- `social/application/eventListener/LabelMemberShipEventListener.java`
- `global/config/AsyncConfig.java` (없으면 신규)

## 주의

`@TransactionalEventListener`와 `@Async`를 함께 사용할 경우,
`AFTER_COMMIT` 페이즈가 비동기 스레드에서도 올바르게 동작하는지 확인한다.
Spring은 기본적으로 이를 지원하나, 트랜잭션 컨텍스트가 전파되지 않으므로
리스너 내부에서 새 트랜잭션(`@Neo4jTransactional` = `REQUIRED`)이 열리는 구조가 맞다.

---

## Result

**브랜치:** `ai/fix-label-cleanup-listener`
**커밋:** `0f9b268`

### 변경 내용

`LabelMemberShipEventListener.java` 어노테이션 3개 교체, 메서드 바디 무변경.

| 전 | 후 |
|----|-----|
| `@EventListener` | `@TransactionalEventListener(phase = AFTER_COMMIT)` |
| `@Transactional` | `@Neo4jTransactional` |
| (없음) | `@Async` |

### 테스트 결과

`LabelMemberShipEventListenerTest`: 4/4 PASSED
