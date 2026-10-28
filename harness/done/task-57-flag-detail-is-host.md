# Task-57: FlagDetailResult에 isHost 필드 추가

## Background

`GET /api/v1/flags/{id}` 응답(`FlagDetailResult`)에 현재 요청자가 호스트인지 여부가 포함되지 않는다.
`FlagQueryController.getFlagDetail()`은 `@CurrentUserId`를 받지 않아
서비스 레이어까지 요청자 ID가 전달되지 않는다.

프론트엔드에서 수정/삭제 버튼, 초대 권한 변경 UI 등을 조건부로 노출하려면
응답에 `isHost: boolean` 필드가 필요하다.

## Objective

- `FlagDetailResult`에 `isHost` 필드 추가
- `GET /api/v1/flags/{id}` 호출 시 요청자 ID와 `flag.hostId`를 비교해 값 설정

## Decision

- `FlagDetailResult`에 `boolean isHost` 필드 추가, `of()` 팩토리 메서드에 파라미터 추가
- `FlagQueryUseCase.getFlagDetail(Long flagId, Long viewerId)`로 시그니처 변경
- `FlagQueryService`: `flag.getHostId().equals(viewerId)`로 계산
- `FlagQueryController`: `@CurrentUserId Long currentUserId` 추가 후 서비스에 전달

## Result

변경 파일 4개, 테스트 파일 3개 수정/신설. 브랜치 `ai/feat-flag-detail-is-host` → main 머지 완료.

**Production (4개)**
- `FlagDetailResult` — `isHost` 필드 및 `of()` 파라미터 추가
- `FlagQueryUseCase` — 시그니처 변경
- `FlagQueryService` — `isHost` 계산 로직 추가
- `FlagQueryController` — `@CurrentUserId` 추가

**Test (3개)**
- `FlagQueryServiceTest` — 기존 호출 5곳 시그니처 수정 + `isHost true/false` 케이스 신규 추가
- `FlagQueryControllerTest` (신규) — 성공(`$.isHost` 검증 + `verify` currentUserId 전달 확인), 404 케이스
- `FlagControllerTest` — 잘못 추가됐던 `getFlagDetail` 테스트 및 관련 import 제거

**후속 버그픽스**

작업 중 `GET /api/v1/flags/{id}` 호출 시 `capacity=null`인 Flag에서 NPE 발생 확인.

```
NullPointerException: Cannot invoke "Integer.intValue()" because
"Flag.getCapacity()" is null  at FlagDetailResult.of(FlagDetailResult.java:34)
```

`Flag.capacity`는 nullable(`Integer`, null = 무제한 인원)인데 `FlagDetailResult`, `FlagResult` 모두
`int capacity`(primitive)로 선언되어 언박싱 시 NPE가 발생했다.
두 DTO의 `int capacity` → `Integer capacity`로 수정.
