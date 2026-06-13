# Task-73: 플래그 접근 제어 방향 수정 (댓글 개방 / 메모리얼 제한)

## 배경 및 목적

서버 로그에서 `GET /api/v1/flags/{id}/comments` 요청이 `FlagAuthorizationException`을 던지는 것을 발견했다.  
반대로 `GET /api/v1/flags/{id}/memorials` 는 비참여자도 호출이 가능해 `locked: true` 응답을 받는 구조였다.

비즈니스 의도와 반대:
- **댓글**: 누구나 조회 가능해야 한다. 비공개 댓글 필터링은 `isVisibleTo`가 처리한다.
- **메모리얼**: 참여자(또는 호스트)만 접근 가능해야 한다. 비참여자는 예외를 받아야 한다.

## 변경 내용

### `FlagCommentQueryService`
- `findAllParticipantIds` 쿼리 및 `flag.validateAccess(viewerId, participantIds)` 호출 제거
- 댓글 조회는 누구나 가능, 비공개 댓글 필터링은 `isVisibleTo`에 위임

### `FlagMemorialQueryService`
- `FlagRepository` 의존성 추가
- `findHostIdById`로 플래그 존재 확인 (없으면 `FlagNotFoundException`)
- `isParticipating`으로 참여 여부 확인 (비참여자는 `FlagAuthorizationException`)
- 기존 메모리얼 작성 여부 체크 (`locked` 로직)는 유지

## 테스트 변경

### `FlagCommentQueryServiceTest`
- 모든 케이스에서 `given(flagRepository.findAllParticipantIds(...))` stub 제거

### `FlagMemorialQueryServiceTest`
- `FlagRepository` mock 추가
- 기존 케이스에 `findHostIdById` / `isParticipating` stub 추가
- 신규 케이스 2개 추가:
  - 존재하지 않는 플래그 → `FlagNotFoundException`
  - 비참여자 조회 → `FlagAuthorizationException`

## 영향 범위

- `FlagCommentQueryService`, `FlagMemorialQueryService`
- 관련 테스트 2개
