# PLAN: Task-59 (ad-hoc) — BuzzCommentResult에 isMine 필드 추가

## 목표

`GET /api/v1/buzzes/{buzzId}` 응답 내 `comments[]` 배열의 각 항목에
`isMine: boolean` 필드 추가. 프론트엔드에서 댓글 삭제 버튼 등을 조건부 노출하는 데 사용한다.

---

## 분석

댓글 목록은 별도 GET 엔드포인트 없이 `BuzzDetailResult.comments`로만 내려온다.
`BuzzDetailResult.from(buzz, currentUserId)` 내부에서 `getVisibleComments(currentUserId)`를 순회해
`BuzzCommentResult::from`으로 매핑하는 구조이며, `currentUserId`가 이미 이 시점에 존재한다.
**컨트롤러/유스케이스/서비스 시그니처 변경 없음.**

---

## 변경 파일 (3개)

### 1. `BuzzCommentResult.java`

`isMine` 필드 추가, `from()` 시그니처에 `viewerId` 추가.

```java
public record BuzzCommentResult(
    String commentId, BuzzProfileResult author,
    String text, List<String> imageUrls,
    LocalDateTime createdAt, boolean isMine
) {
    public static BuzzCommentResult from(BuzzComment comment, Long viewerId) {
        return new BuzzCommentResult(..., comment.getCommenterId().equals(viewerId));
    }
}
```

### 2. `BuzzDetailResult.java`

comment 매핑을 메서드 레퍼런스에서 람다로 변경하여 `viewerId` 전달.

```java
// before
.map(BuzzCommentResult::from)
// after
.map(c -> BuzzCommentResult.from(c, currentUserId))
```

### 3. `BuzzComment.java`

`isCommenter(Long userId)` 도메인 메서드 추가 — `Buzz.isCreator()` 패턴과 통일.

```java
public boolean isCommenter(Long userId) {
    return commenterId.equals(userId);
}
```

---

## 영향 범위

### Production 코드
- 컨트롤러/유스케이스/서비스 시그니처 변경 없음

### 테스트 코드
| 파일 | 작업 |
|------|------|
| `BuzzCommentTest.java` | `isCommenter()` true/false 케이스 추가 |
| `BuzzServiceTest.java` | `getBuzzDetail` 결과 `comments[].isMine` 검증 케이스 추가 |
| `BuzzControllerTest.java` | `$.comments[0].isMine` jsonPath 검증 추가 |

## 브랜치
`ai/feat-buzz-comment-is-mine` (from main)
