# Task-63: Trace reveal 결과를 HTTP 응답으로 반환

## Background

현재 `POST /api/v1/social/traces`는 `204 No Content`를 반환한다.
reveal이 발생하면 `TraceRevealedEvent` → `TraceEventListener` (async) → `NotificationEvent` 경로로
방문한 유저(visitor)와 방문당한 유저(visitee) 모두에게 FCM 알림이 전송된다.

### 문제

reveal 알림이 **비동기 이벤트에만 의존**하고 있어, 이벤트 유실 시 visitor는 reveal이 발생했다는 사실을
영원히 알 수 없다. reveal은 핵심 소셜 기능(서로 관심이 있었음을 확인하는 순간)이므로 신뢰성이 중요하다.

### 설계 방향

- **Visitor**: HTTP 응답으로 reveal 여부를 동기적으로 전달 → 이벤트 유실과 무관하게 안전
- **Visitee**: 기존 async 알림 그대로 유지
- reveal은 **"순간"** 이다. reveal이 처음 터진 그 요청에서만 `revealed: true`를 반환하고,
  이후 방문에서는 `revealed: false`를 반환한다 (이미 알려줬으므로 반복하지 않는다).

## Objective

`POST /api/v1/social/traces` 응답을 `204 No Content` → `200 OK + TraceResult`로 변경한다.
`TraceResult.revealed`는 해당 요청이 reveal을 발생시킨 경우에만 `true`다.

## 변경 범위

### 1. `TraceCommandUseCase`

```java
TraceResult recordTrace(Long visitorId, Long targetId);
```

반환 타입 `void` → `TraceResult`.

### 2. `TraceResult` (신규 DTO)

```java
// trace/application/dto/TraceResult.java
public record TraceResult(boolean revealed, Long revealedWithUserId) {}
```

reveal이 없으면 `TraceResult(false, null)`.

### 3. `TraceService`

```java
boolean wasRevealed = trace.isRevealed();
trace.recordVisit(visitorId);
traceRepository.save(trace);

boolean justRevealed = !wasRevealed && trace.isRevealed();
Long otherUserId = justRevealed ? getOtherUserId(trace, visitorId) : null;
return new TraceResult(justRevealed, otherUserId);
```

before/after 비교로 "이 호출이 reveal을 유발했는가"를 판별한다.

### 4. `TraceController`

```java
// 기존
@ResponseStatus(HttpStatus.NO_CONTENT)
public void recordTrace(...) { ... }

// 변경
public ResponseEntity<TraceResult> recordTrace(...) {
    TraceResult result = traceCommandUseCase.recordTrace(currentUserId, request.targetId());
    return ResponseEntity.ok(result);
}
```

### 5. `TraceControllerTest`

- `status().isNoContent()` → `status().isOk()`
- `revealed: true` / `revealed: false` 응답 검증 케이스 추가

## 결정 사항

- **`TraceRevealedEvent` async 알림은 그대로 유지**: visitee는 알림으로, visitor는 HTTP 응답으로 이중 처리
- **Outbox 패턴 미적용**: visitor는 HTTP 응답으로 이미 커버되며, visitee 알림 유실은 소셜 앱 수준에서 감내 가능
- **`revealed: true`는 최초 1회만**: `!wasRevealed && trace.isRevealed()` 조건으로 보장
