# Task-120: Buzz 댓글 검증이 전제하는 목록을 투영에서 분리한다

## Background

`BuzzComment`는 `Buzz` 도큐먼트의 `comments` 배열에 임베딩돼 있고, 댓글의 생성·수정·삭제 검증을 `Buzz`가 전담한다.

`BuzzService`의 댓글 명령은 전부 `getBuzzOrThrow` 한 경로로 Buzz를 읽는데, 그 경로가 `BuzzRepositoryAdapter.findById` → `findByIdWithCommentSlice`다. 명령 처리에 쓰이는 Buzz가 `$slice`로 마지막 20개만 잘라온 투영이고, 그 위에서 `Buzz.findComments(commentId)`가 대상 댓글을 찾는다.

그래서 한 버즈에 댓글이 21개를 넘기면 밀려난 댓글은 작성자 본인이어도 수정과 삭제가 `BuzzCommentNotFoundException`으로 떨어진다. `getVisibleComments`도 그 20개만 돌려주고 더 받아올 API가 없다.

데이터가 사라지는 것은 아니다. 도큐먼트에는 전부 들어 있고 투영만 잘린다. 재현에는 생성 후 6시간 안에 한 버즈에 21개 이상이 쌓여야 한다. TTL이 사실상 상한 역할을 해서 빈도는 낮지만 구조적으로 막혀 있지는 않다.

같은 뿌리에서 나온 중복이 둘 더 있다.

- `BuzzService`가 `validateCommentCreation`을 부른 뒤 `createComment`를 부르는데 `createComment`가 같은 검증을 다시 한다. 수정 경로도 `validateCommentUpdate`와 `updateComment`가 겹친다.
- `Buzz.createComment`가 메모리에서 `markAsRead`를 하고, 어댑터 `addComment`가 `addToSet`으로 같은 일을 다시 한다. 읽음 처리 규칙이 도메인과 어댑터 양쪽에 있다.

## Objective

투영으로 읽어온 Buzz에 "댓글 목록 전체를 안다"를 전제하는 검증을 없앤다. 밀려난 댓글도 권한만 맞으면 수정·삭제된다. 저장 구조와 API 응답 형태는 바뀌지 않는다.

## Decision

### 경계상 Buzz와 BuzzComment는 서로 다른 애그리거트다

둘 사이에 형제 집합 규칙이 없다. 댓글 수 상한이 없고, 만료 여부와 수신자 자격은 Buzz 자신의 상태만 읽으면 판정된다. 형제를 세거나 비교해야 나오는 규칙이 하나도 없으므로 같은 애그리거트로 묶을 근거가 없다.

저장을 한 도큐먼트에 하는 것과 애그리거트 경계는 별개다. Buzz는 이미 `creatorNickname`처럼 account 애그리거트의 값을 복사해 들고 있고, 그것 때문에 두 애그리거트가 하나가 되지는 않는다. BuzzComment도 같은 성격으로 본다.

### 저장 구조는 그대로 둔다

임베딩, MongoDB, `$slice`를 모두 유지한다. 수명이 6시간이고 상세 화면에서 항상 함께 읽히므로 임베딩이 맞다.

MySQL 이관은 기각한다. Buzz가 쓰는 배열이 `recipientIds`(최대 150), `readRecipientIds`, `imageUrls`, `comments` 넷이라 테이블 넷이 늘고, "내가 수신자인 버즈 목록"이라는 핵심 조회가 멀티키 인덱스 한 번에서 조인과 페이징으로 바뀐다. TTL은 `FlagPurgeScheduler`와 같은 방식으로 대체 가능하므로 이관의 근거가 되지 못한다. notification도 MongoDB를 쓰고 있어 buzz만 옮기면 부분 이관이 된다.

### Buzz는 댓글을 찾지 않는다

Buzz는 만료 여부와 접근 자격만 판정한다. 대상 댓글의 식별과 작성자 판정은 그 댓글을 실제로 가진 쪽이 맡는다. 댓글 객체를 넘겨받을지 좁은 업데이트 쿼리의 매칭 조건으로 처리할지는 PLAN에서 정한다.

### 중복된 검증과 읽음 처리를 한 곳으로 모은다

서비스와 도메인에 두 번 있는 검증은 하나만 남긴다. 읽음 처리도 도메인과 어댑터 중 한 곳에서만 한다.

## Out of Scope

- MongoDB에서 MySQL로의 이관
- 댓글 페이징 API 추가. 21번째 이전 댓글이 상세 응답에 보이지 않는 것은 이 태스크에서 다루지 않는다.
- `$slice` 크기 변경
- TTL 정책과 만료 시간
- 댓글 공개 범위(`isPublic`) 정책

## Acceptance Criteria

- 댓글이 25개인 버즈에서 가장 오래된 댓글을 그 작성자가 수정·삭제할 수 있다.
- 위 회귀 테스트가 수정 전 동작에서 실패하는 것을 확인한다.
- 만료된 버즈, 수신자가 아닌 사용자, 작성자가 아닌 사용자의 수정 시도는 변경 전과 같은 예외로 막힌다.
- 댓글 삭제는 변경 전과 같이 작성자와 버즈 생성자 모두 할 수 있다.
- `Buzz`에 댓글 목록 전체를 전제하는 메서드가 남지 않는다.
- 읽음 처리 규칙이 도메인과 어댑터 양쪽에 중복으로 있지 않다.
- 상세 조회 응답의 댓글 개수, 순서, 공개 범위 필터링이 변경 전과 같다.
