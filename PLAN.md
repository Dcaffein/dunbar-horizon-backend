# PLAN.md — Task-83: TraceRevealed 알림에 상대방 프로필 메타데이터 포함

## 작업 목표

`TRACE_REVEALED` 알림 수신 시 프론트엔드가 상대방 프로필 페이지로 바로 라우팅할 수 있도록,
수신자별로 이벤트를 분리하고 메타데이터에 상대방 프로필(`senderUserId`, `senderNickname`, `senderProfileImageUrl`)을 포함한다.

---

## 현황 분석

| 항목 | 현재 상태 | 문제 |
|------|-----------|------|
| `TraceEventListener` | `.receiverId(minId).receiverId(maxId)` 단일 이벤트 발행 | 메타데이터가 공유되어 수신자별 다른 프로필 담기 불가 |
| `NotificationEvent.metadata` | 빈 맵 `{}` | 프론트 라우팅에 필요한 정보 없음 |
| `NotificationEventListener` | `receiverIds`를 순회하며 Notification 문서 각각 생성 | 이벤트만 분리되면 별도 수정 불필요 |

---

## 변경 파일 목록

### 1. `trace/application/TraceEventListener.java`

- `UserQueryUseCase` 주입
- `getUserProfiles(List.of(minId, maxId))`로 두 유저 프로필 한 번에 조회
- 조회 결과 중 한쪽이라도 없으면 `log.warn` 후 발송 중단
- 단일 이벤트 → 2개 분리 발행
  - `receiverId = minId`, metadata = maxUser 프로필
  - `receiverId = maxId`, metadata = minUser 프로필
- 메타데이터 구조:
  ```json
  {
    "senderUserId": 2,
    "senderNickname": "닉네임",
    "senderProfileImageUrl": "https://..." 
  }
  ```
  (`profileImageUrl`이 null이면 `""`)

### 2. `trace/TraceEventListenerTest.java`

- `@Mock UserQueryUseCase` 추가
- `getUserProfiles` stub — `UserProfileInfo(1L, "nickA", "imgA")`, `UserProfileInfo(2L, "nickB", "imgB")` 반환
- `verify(eventPublisher, times(2))` 로 수정
- `ArgumentCaptor`로 캡처한 두 이벤트 각각 검증
  - 각 이벤트의 `receiverIds` 단건 확인
  - 각 이벤트의 metadata에 상대방 `senderUserId` 포함 확인
- 엣지 케이스 추가: 프로필 조회 실패 시 `publishEvent` 미호출 검증

---

## 구현 방향

- `trace` → `account` 의존은 `UserQueryUseCase` 인터페이스(포트)를 통해서만
- `Map.of(...)` 사용 (불변 맵, null value 불허) → `profileImageUrl` null 방어 필수
- `buildNotificationEvent(Long receiverId, UserProfileInfo sender)` private 헬퍼로 분리

---

## 예상 사이드 이펙트

- 기존 단일 이벤트 → 2개 분리로 MongoDB에 Notification 문서가 2개 생성됨 (기존과 동일한 의도)
- 프론트엔드는 metadata에 `senderUserId` 존재 여부로 라우팅 가능 여부를 판단해야 함

---

## 테스트 전략

단위 테스트(Mockito) 수정만으로 충분 — 기존 `TraceEventListenerTest` 수정 및 엣지 케이스 1개 추가.
