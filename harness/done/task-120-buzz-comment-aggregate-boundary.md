# Task-120: Buzz 댓글 검증이 전제하는 목록을 투영에서 분리한다

## Background

`BuzzComment`는 `Buzz` 도큐먼트의 `comments` 배열에 임베딩돼 있고, 댓글 검증을 `Buzz`가 전담한다.

댓글 명령이 전부 `getBuzzOrThrow` 한 경로로 Buzz를 읽는데, 그 경로는 응답 크기를 줄이려고 `$slice`로 마지막 20개만 잘라오는 상세 조회용이다. 그 투영 위에서 `Buzz.findComments(commentId)`가 대상을 찾는다. 그래서 댓글이 21개를 넘기면 밀려난 댓글은 작성자 본인이어도 수정과 삭제가 `BuzzCommentNotFoundException`으로 떨어진다.

데이터는 도큐먼트에 그대로 있고 투영만 잘린다. 재현에는 6시간 안에 한 버즈에 21개 이상이 쌓여야 해서 빈도는 낮다.

같은 뿌리에서 나온 중복이 둘 더 있다. 서비스가 `validateCommentCreation`을 부른 뒤 `createComment`가 같은 검증을 다시 한다. 그리고 `createComment`가 메모리에서 `markAsRead`를 하는데 실제 저장은 어댑터 `addComment`의 `addToSet`이 따로 한다.

## Objective

투영으로 읽어온 Buzz에 "댓글 목록 전체를 안다"를 전제하는 검증을 없앤다. 밀려난 댓글도 권한만 맞으면 수정·삭제된다. 저장 구조와 API 응답 형태는 바뀌지 않는다.

## Decision

### 저장 구조는 그대로 둔다

임베딩, MongoDB, `$slice`를 모두 유지한다. 수명이 6시간이고 상세 화면에서 항상 함께 읽히므로 임베딩이 맞다.

### buzz 댓글만 부모에게 묻는 분기를 없앤다

flag는 대상 댓글을 `commentRepository.findById(commentId)`로 직접 읽고, 작성자 판정은 `FlagComment`가 하며 `Flag`는 `hostId`만 건넨다. 같은 개념인 buzz 댓글만 부모에게 묻고 있고, 그 차이를 만든 것은 도메인이 아니라 저장소 선택이다.

임베딩은 저장 선택이지 코드 모델을 정하지 않는다. Buzz가 댓글의 수명을 소유하는 것은 맞지만, 그 소유가 "대상 댓글을 Buzz에게 물어봐야 한다"를 요구하지는 않는다.

### Buzz는 댓글을 찾지 않는다

Buzz는 만료 여부와 접근 자격만 판정한다. 대상 댓글은 저장소에서 하나만 읽어오고 `BuzzComment`가 스스로 작성자를 판정한다. 단 `BuzzComment`는 `(buzzId, commentId)`로만 주소가 잡히므로 flag처럼 id 하나로 읽을 수는 없다.

좁은 업데이트 쿼리의 매칭 조건(`commentId` + `commenterId`)으로 처리하는 쪽은 기각한다. 매칭 0건이 "댓글이 없다"인지 "작성자가 아니다"인지 구분되지 않아 404와 403을 나눠 던질 수 없다.

만료 검사는 대상 댓글 조회보다 먼저 한다. 순서가 뒤바뀌면 만료된 버즈에서 없는 댓글을 수정할 때 예외가 400에서 404로 바뀐다.

### 검증과 읽음 처리는 각각 한 곳에서만 한다

자격 판정은 `Buzz.validateCommentCreation`이, 댓글 생성은 `BuzzComment.create`가 맡는다.

읽음 처리는 판정과 저장을 나눈다. 수신자일 때만 읽음으로 친다는 규칙은 도메인(`Buzz.isRecipient`)이 말하고, 저장은 어댑터의 단일 업데이트가 한다. 지금은 어댑터가 무조건 `addToSet`을 걸어 버즈 작성자도 `readRecipientIds`에 들어가는데 도메인 쪽으로 맞춘다. `isUnreadBy`와 `findUnreadSenderIds`가 모두 수신자 기준이라 외부 동작 변화는 없다.

## Out of Scope

- 댓글 페이징 API 추가. 21번째 이전 댓글이 상세 응답에 보이지 않는 것은 여기서 다루지 않는다.
- `$slice` 크기 변경
- TTL 정책과 만료 시간
- 댓글 공개 범위(`isPublic`) 정책
- 목록 조회가 `commentCount` 때문에 댓글 전체를 읽는 문제

## Acceptance Criteria

- 댓글이 25개인 버즈에서 가장 오래된 댓글을 그 작성자가 수정·삭제할 수 있다.
- 위 회귀 테스트가 수정 전 동작에서 실패하는 것을 확인한다.
- 만료된 버즈, 수신자가 아닌 사용자, 작성자가 아닌 사용자의 수정 시도는 변경 전과 같은 예외로 막힌다.
- 댓글 삭제는 변경 전과 같이 작성자와 버즈 생성자 모두 할 수 있다.
- `Buzz`에 대상 댓글을 찾는 메서드가 남지 않는다. 로드된 댓글을 공개 범위로 거르는 `getVisibleComments`는 해당하지 않는다.
- 읽음 처리 규칙이 도메인과 어댑터 양쪽에 중복으로 있지 않다.
- 상세 조회 응답의 댓글 개수, 순서, 공개 범위 필터링이 변경 전과 같다.
