# PLAN: Task-58 — Buzz 응답에 isCreator 필드 추가

## 목표

`BuzzDetailResult`, `BuzzSummaryResult` 응답에 `isCreator: boolean` 필드 추가.
프론트엔드에서 Buzz 삭제 버튼, 댓글 삭제 권한 UI를 조건부 노출하는 데 사용한다.

---

## 분석

`currentUserId`는 이미 컨트롤러 → 서비스 → `from(buzz, userId)` 팩토리까지 흐르고 있다.
**컨트롤러, 유스케이스, 서비스 시그니처 변경 없음.** DTO와 도메인 메서드만 추가하면 된다.

`creatorId.equals(userId)` 패턴이 도메인 메서드 5곳에 인라인으로 흩어져 있어
`isCreator()` 도메인 메서드로 통일한다.

---

## 변경 파일 (3개)

### 1. `Buzz.java`

`isCreator(Long userId)` 도메인 메서드 추가 + 기존 인라인 `creatorId.equals(userId)` 호출부 교체.

```java
public boolean isCreator(Long userId) {
    return creatorId.equals(userId);
}
```

교체 대상 (5곳):
- `createComment()` — `!creatorId.equals(commenterId)` → `!isCreator(commenterId)`
- `validateCommentDeletion()` — `!this.creatorId.equals(requesterId)` → `!isCreator(requesterId)`
- `validateAccess()` — `!creatorId.equals(userId)` → `!isCreator(userId)`
- `validateDeletion()` — `!creatorId.equals(requesterId)` → `!isCreator(requesterId)`
- `getVisibleComments()` — `creatorId.equals(viewerId)` → `isCreator(viewerId)`

### 2. `BuzzDetailResult.java`

`isCreator` 필드 추가, `from()` 수정.

```java
public record BuzzDetailResult(
    String buzzId, BuzzProfileResult author, String text,
    List<String> imageUrls, List<BuzzCommentResult> comments,
    long remainingMinutes, boolean isUnread, boolean isCreator   // 추가
) {
    public static BuzzDetailResult from(Buzz buzz, Long currentUserId) {
        return new BuzzDetailResult(..., buzz.isCreator(currentUserId));
    }
}
```

### 3. `BuzzSummaryResult.java`

`isCreator` 필드 추가, `from()` 수정.

```java
public record BuzzSummaryResult(
    String buzzId, BuzzProfileResult author, String text,
    List<String> imageUrls, int commentCount,
    long remainingMinutes, boolean isUnread, boolean isCreator   // 추가
) {
    public static BuzzSummaryResult from(Buzz buzz, Long currentUserId) {
        return new BuzzSummaryResult(..., buzz.isCreator(currentUserId));
    }
}
```

---

## 영향 범위

### Production 코드
- 컨트롤러/유스케이스/서비스 시그니처 변경 없음
- `Buzz.java` 내부 리팩터링 + 도메인 메서드 추가
- `BuzzDetailResult`, `BuzzSummaryResult` 필드 추가

### 테스트 코드
| 파일 | 작업 |
|------|------|
| `BuzzTest.java` | `isCreator()` 도메인 메서드 테스트 추가 (creator=true, non-creator=false) |
| `BuzzServiceTest.java` | `getBuzzDetail` 결과의 `isCreator` 검증 케이스 추가 |
| `BuzzControllerTest.java` | 변경 없음 (컨트롤러 시그니처 불변) |

## 브랜치
`ai/feat-buzz-is-creator` (from main)
