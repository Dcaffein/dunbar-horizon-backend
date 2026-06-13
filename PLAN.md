# PLAN — Task 70: 앵콜 초대장 알림 메시지 차별화

## 작업 목표

앵콜로 발송된 초대장 알림 메시지를 일반 초대장과 구분한다.
`FlagInvitationSentEvent`에 `isEncore` 필드를 추가하고,
`FlagInvitationEventListener`에서 메시지를 분기한다.

## 현황 분석

### FlagInvitationSentEvent (현재)

```java
public record FlagInvitationSentEvent(
        Long flagId,
        Long invitationId,
        Long inviteeId,
        String flagTitle
) {}
```

### FlagInvitationEventListener (현재)

```java
NotificationEvent.builder()
    .title("플래그 초대")
    .content(String.format("[%s] 플래그에 초대받았습니다. 수락 여부를 선택해주세요!", event.flagTitle()))
    ...
```

앵콜/일반 구분 없이 동일한 메시지 사용.

### 이벤트 발행 지점 2곳

| 발행처 | 파일 | 앵콜 여부 |
|--------|------|---------|
| 일반 초대 | `FlagInvitationService.invite()` | false |
| 앵콜 자동 초대 | `FlagEncoreInvitationListener.handle()` | true |

## 구현 계획

### 1. `FlagInvitationSentEvent` — `isEncore` 필드 추가

```java
public record FlagInvitationSentEvent(
        Long flagId,
        Long invitationId,
        Long inviteeId,
        String flagTitle,
        boolean isEncore
) {}
```

### 2. `FlagInvitationEventListener` — 메시지 분기

```java
String title   = event.isEncore() ? "앵콜 초대" : "플래그 초대";
String content = event.isEncore()
        ? String.format("[%s] 앵콜 모임에 초대받았습니다. 함께하실래요!", event.flagTitle())
        : String.format("[%s] 플래그에 초대받았습니다. 수락 여부를 선택해주세요!", event.flagTitle());
```

### 3. `FlagInvitationService` — `isEncore = false` 전달

```java
eventPublisher.publishEvent(new FlagInvitationSentEvent(
        flagId, saved.getId(), inviteeId, flagTitle, false
));
```

### 4. `FlagEncoreInvitationListener` — `isEncore = true` 전달

```java
new FlagInvitationSentEvent(inv.getFlagId(), inv.getId(), inv.getInviteeId(), event.title(), true)
```

## 테스트 계획

### `FlagInvitationEventListenerTest` (신규, Mockito)

| 케이스 | 검증 |
|--------|------|
| `isEncore = false` | 제목 "플래그 초대", 본문에 "수락 여부" 포함 |
| `isEncore = true`  | 제목 "앵콜 초대", 본문에 "앵콜 모임" 포함 |

### `FlagEncoreInvitationListenerTest` — 기존 정상 케이스 수정

`FlagInvitationSentEvent` 필드 추가로 생성자 시그니처가 변경되므로 captor 검증 수정 불필요.
단, `isEncore = true`인지 검증하는 assertion 추가.

## 변경 파일 요약

| 파일 | 변경 유형 |
|------|---------|
| `flag/domain/invitation/event/FlagInvitationSentEvent.java` | 필드 추가 |
| `flag/application/eventListener/FlagInvitationEventListener.java` | 메시지 분기 |
| `flag/application/service/flag/FlagInvitationService.java` | `isEncore = false` |
| `flag/application/eventListener/FlagEncoreInvitationListener.java` | `isEncore = true` |
| `flag/application/eventListener/FlagInvitationEventListenerTest.java` | 신규 |
| `flag/application/eventListener/FlagEncoreInvitationListenerTest.java` | 검증 보강 |
