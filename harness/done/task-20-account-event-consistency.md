# Task 20: AccountCleanupEventListener 트랜잭션 정합성 수정

## Objective

`AccountCleanupEventListener`의 불필요한 `REQUIRES_NEW` 전파 설정을 제거하고 `@Async`를 추가하여
메인 플로우 블로킹을 방지한다.

## Domain Change
[ ] 없음

---

## 배경

```java
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void cleanUpGarbageAuths(UserActivatedEvent event) {
    authRepository.deleteUnverifiedByUserId(event.userId());
}
```

`AFTER_COMMIT` 단계에서는 원본 트랜잭션이 이미 종료되어 있으므로 `REQUIRES_NEW`와 `REQUIRED`의
동작 차이가 없다. 또한 `@Async` 없이 동기 실행되어 메인 플로우를 불필요하게 블로킹한다.

---

## 알려진 제약

- Auth 가비지가 남아있어도 `Auth.isVerified()` 체크가 2차 방어선 역할을 하므로 보안 위협 없음.
  `AFTER_COMMIT` + `@Async` 조합으로 비동기 처리해도 무방하다.
