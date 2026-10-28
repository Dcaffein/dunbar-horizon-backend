# Task-58: Buzz 응답에 isCreator 필드 추가

## Background

`BuzzDetailResult`, `BuzzSummaryResult` 응답에 현재 요청자가 해당 Buzz의 작성자인지
나타내는 필드가 없다. 프론트엔드에서 댓글 삭제 권한, Buzz 삭제 버튼 등을 조건부로
노출하려면 `isCreator: boolean` 필드가 필요하다.

`Buzz` 도메인에는 `isRecipient(userId)`는 있지만 creator 체크 메서드가 없어
`creatorId.equals(userId)`를 서비스·도메인 코드 곳곳에 인라인으로 흩어져 있다.

## Objective

- `Buzz.isCreator(Long userId)` 도메인 메서드 추가
- `BuzzDetailResult`, `BuzzSummaryResult`에 `isCreator` 필드 추가

## Decision

- `Buzz.isCreator(Long userId)` 도메인 메서드 추가, 기존 `creatorId.equals()` 인라인 5곳 교체
- `BuzzDetailResult`, `BuzzSummaryResult` `from()` 팩토리 내부에서 `buzz.isCreator(currentUserId)` 호출 — 컨트롤러/유스케이스/서비스 시그니처 변경 없음 (`currentUserId`가 이미 전달되고 있었음)

## Result

변경 파일 3개, 테스트 파일 3개 수정/신설. 브랜치 `ai/feat-buzz-is-creator` → main 머지 완료.

**Production (3개)**
- `Buzz` — `isCreator(Long userId)` 도메인 메서드 추가, 내부 인라인 `creatorId.equals()` 5곳 교체
- `BuzzDetailResult` — `isCreator` 필드 추가
- `BuzzSummaryResult` — `isCreator` 필드 추가 + `remainingMinutes` 음수 버그(`Math.max(0L, ...)` 누락) 수정

**Test (3개)**
- `BuzzTest` — `isCreator()` true/false 케이스 신규 추가
- `BuzzServiceTest` — `getBuzzDetail` 결과의 `isCreator` 검증 케이스 2개 신규 추가
- `BuzzControllerTest` — `GET /api/v1/buzzes/{buzzId}` 응답 `$.isCreator` jsonPath 검증 케이스 2개 신규 추가
