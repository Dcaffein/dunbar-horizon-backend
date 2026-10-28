# Task-60: BuzzCommentResult에 isMine 필드 추가

## Background

`GET /api/v1/buzzes/{buzzId}` 응답의 `BuzzDetailResult.comments[]` 배열에
현재 요청자가 해당 댓글의 작성자인지 나타내는 필드가 없다.
댓글 목록은 별도 GET 엔드포인트 없이 `BuzzDetailResult.comments`로만 내려오는 구조이며,
프론트엔드에서 댓글 삭제 버튼 등을 조건부로 노출하려면 `isMine: boolean` 필드가 필요하다.

task-58에서 `BuzzDetailResult.isCreator`, `BuzzSummaryResult.isCreator`를 추가한 후
동일한 패턴으로 댓글 단위에도 적용이 필요함이 확인되었다.

## Objective

- `BuzzComment.isCommenter(Long userId)` 도메인 메서드 추가
- `BuzzCommentResult`에 `isMine` 필드 추가

## Decision

- `BuzzComment.isCommenter(Long userId)` 추가 — `Buzz.isCreator()` 패턴과 통일
- `BuzzCommentResult.from(comment, viewerId)` 시그니처 변경, `isMine` 필드 추가
- `BuzzDetailResult.from()` 내부 매핑 람다 변경 — `currentUserId`가 이미 존재하여 컨트롤러/유스케이스/서비스 시그니처 변경 없음

## Result

변경 파일 3개, 테스트 파일 3개 수정. 브랜치 `ai/feat-buzz-comment-is-mine` → main 머지 완료.

**Production (3개)**
- `BuzzComment` — `isCommenter(Long userId)` 도메인 메서드 추가
- `BuzzCommentResult` — `isMine` 필드 추가, `from()` 시그니처에 `viewerId` 추가
- `BuzzDetailResult` — comment 매핑 `BuzzCommentResult::from` → `c -> BuzzCommentResult.from(c, currentUserId)`

**Test (3개)**
- `BuzzCommentTest` — `isCommenter()` true/false 케이스 추가
- `BuzzServiceTest` — 본인/타인 댓글 `isMine` 검증 케이스 2개 추가
- `BuzzControllerTest` — `$.comments[0].isMine` jsonPath 검증 추가
