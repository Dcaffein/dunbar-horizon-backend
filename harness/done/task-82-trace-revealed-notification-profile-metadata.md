# Task-82: TraceRevealed 알림에 상대방 프로필 메타데이터 포함

## 배경 및 목적

`TRACE_REVEALED` 알림 수신 시 프론트엔드가 상대방 프로필 페이지로 바로 라우팅할 수 있도록,
알림 메타데이터에 상대방의 `userId`, `nickname`, `profileImageUrl`을 포함한다.

현재 `TraceEventListener`는 두 유저를 하나의 `NotificationEvent`에 묶어 동일한 메타데이터를 전달한다.
A에게는 B의 프로필이, B에게는 A의 프로필이 담겨야 하므로 이벤트를 수신자별로 분리해야 한다.

---

## 현황 및 문제

- `TraceEventListener.handleTraceRevealed()`가 `.receiverId(minId).receiverId(maxId)`로 단일 이벤트 발행
- `metadata`가 공유되므로 수신자별로 다른 프로필 정보를 담을 수 없음
- `metadata`가 현재 빈 맵(`{}`)

---

## 변경 방향

### 이벤트 분리

단일 `NotificationEvent` → 수신자별 2개의 `NotificationEvent`로 분리

- minId 수신 이벤트: metadata에 maxId 유저 프로필
- maxId 수신 이벤트: metadata에 minId 유저 프로필

### 메타데이터 구조

```json
{
  "senderUserId": 2,
  "senderNickname": "닉네임",
  "senderProfileImageUrl": "https://..."
}
```

`profileImageUrl`이 null인 경우 빈 문자열(`""`)로 전달한다.

### 프로필 조회

`trace.application.TraceEventListener`에서 `account` 도메인의 `UserQueryUseCase`를 주입하여
`getUserProfiles(List.of(minId, maxId)`로 두 유저 프로필을 한 번에 조회한다.

---

## 변경 파일 목록

| 파일 | 변경 내용 |
|------|---------|
| `trace/application/TraceEventListener.java` | `UserQueryUseCase` 주입, 이벤트 2개 분리, 메타데이터 포함 |
| `trace/TraceEventListenerTest.java` | 이벤트 2회 발행 및 각 메타데이터 검증으로 테스트 수정 |

---

## 핵심 제약사항

| 항목 | 요구사항 |
|------|---------|
| 이벤트 분리 | 수신자별 별도 `NotificationEvent` 발행 |
| 프로필 조회 실패 | 어느 한쪽이라도 조회 실패 시 알림 발송 중단, 경고 로그 |
| 크로스 도메인 | `trace` → `account` 의존은 UseCase 인터페이스(포트)를 통해서만 허용 |

---

## Out of Scope

- `TraceRevealedEvent` 구조 변경
- 알림 메타데이터 스키마 버전 관리
