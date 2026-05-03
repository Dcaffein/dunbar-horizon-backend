## Objective
서로 호감이 확인(Reveal)되었을 때 방문자만 알림을 받는 버그를 고쳐 양측 모두 알림을 받을 수 있게 한다. 
또한 API 응답 구조를 없애고 순수 이벤트 기반 구조로 전환하여 프론트엔드의 체감 속도와 장애 격리를 개선한다.

## Domain Change
[ ] 없음

## Target Files
- `{trace}/application/TraceEventListener.java` — 양측 알림 발송으로 수정
- `{trace}/application/TraceService.java` — 반환 로직 제거
- `{trace}/adapter/in/web/TraceController.java` — API 스펙 변경
- `{trace}/adapter/in/web/dto/TraceRecordResponseDto.java` — DTO 삭제

## Requirements
1. `TraceEventListener`에서 `TraceRevealedEvent` 수신 시, `userAId`와 `userBId` 양측 모두에게 알림을 발송하도록 수정한다. 이벤트는 기존과 동일하게 1회 발행되며, 발행 구조(`Trace.checkAndReveal`)는 변경하지 않는다.
2. `TraceService`의 반환형을 `void`로 변경한다.
3. `TraceRecordResponseDto` 클래스를 삭제하고, 컨트롤러는 결과 Body 없이 200 OK 상태 코드만 반환한다.

## Decisions
| 항목 | 결정값 | 비고 |
|------|--------|------|
| 알림 이벤트 | `TraceRevealedEvent(minId, maxId)` — 필드명 변경 포함 | 두 참여자를 대칭으로 표현, ID 크기 순 의미를 명시 |
| 양측 알림 책임 | `TraceEventListener` | 이벤트 1회 수신 후 userAId·userBId 각각에 발송 |

## API Contract Change
[x] 있음

변경 전: POST `/api/v1/traces` — 응답: `TraceRecordResponseDto` 반환
변경 후: POST `/api/v1/traces` — 응답: HTTP Status 200 OK (Body 없음)