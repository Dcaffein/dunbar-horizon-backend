# PLAN: task-44 — Buzz 도메인 버그 수정 및 도메인 규칙 강화

## 작업 목표

Buzz 도메인에서 발견된 버그 4건을 수정하고, 서비스 계층에 흩어진 권한 검증 로직을 도메인으로 이동한다.

---

## 현황 분석

| # | 위치 | 문제 |
|---|------|------|
| 1 | `BuzzDetailResult.from()` L24 | `buzz.getComments()` 전체를 그대로 반환 — `isPublic=false` 댓글이 모두에게 노출 |
| 2 | `BuzzNotificationDispatcher.dispatch(BuzzCommentedEvent)` | `creatorId == commenterId` 조건 없음 — 작성자가 자신의 Buzz에 댓글 달면 자기 자신에게 알림 |
| 3 | `BuzzService.commentOnBuzz` L74, `updateComment` L91 | `upload()` 이후 도메인 검증 — 만료/권한 오류 시 S3 파일 고아 발생 |
| 4 | `Buzz.validateCommentDeletion()` L120 | `isExpired()` 체크 없음 — `createComment`, `updateComment`와 불일치 |
| 5 | `BuzzService.deleteBuzz` L106, `getBuzzDetail` L123 | 권한 검증 로직이 서비스 계층에 인라인 위치 |

---

## 변경 파일 목록

| 파일 | 변경 내용 |
|------|-----------|
| `buzz/domain/Buzz.java` | `getVisibleComments(Long viewerId)` 추가, `validateAccess` / `validateDeletion` / `validateCommentCreation` / `validateCommentUpdate` 추가, `validateCommentDeletion`에 만료 체크 추가 |
| `buzz/application/dto/result/BuzzDetailResult.java` | `getComments()` → `getVisibleComments(currentUserId)` |
| `buzz/application/service/BuzzService.java` | upload 순서 변경, 권한 검증 도메인 위임 |
| `buzz/application/eventListener/BuzzNotificationDispatcher.java` | 자기 자신 알림 가드 추가 |

---

## 구현 방향

### isPublic 필터

`Buzz`에 `getVisibleComments(Long viewerId)` 추가:
```java
public List<BuzzComment> getVisibleComments(Long viewerId) {
    return comments.stream()
            .filter(c -> c.isPublic() || creatorId.equals(viewerId) || c.getCommenterId().equals(viewerId))
            .toList();
}
```

`BuzzDetailResult.from()`에서 `buzz.getComments()` → `buzz.getVisibleComments(currentUserId)`.

### S3 업로드 순서

`commentOnBuzz`: `validateCommentCreation()` (만료 + 접근 검증) → `upload()` → `createComment()`  
`updateComment`: `validateCommentUpdate()` (만료 + 작성자 검증) → `upload()` → persist

`createComment()` 내부 검증은 그대로 유지 — 사전 검증 메서드는 upload 이전 guard 역할만.

### 권한 검증 도메인 이동

`Buzz`에 `validateAccess(Long userId)`, `validateDeletion(Long requesterId)` 추가.  
서비스의 인라인 `if (!...) throw` 블록을 해당 메서드 호출로 교체.

---

## 예상 사이드 이펙트

- `getBuzzDetail` 응답에서 `isPublic=false` 댓글이 필터링되므로, 댓글 작성자 본인과 Buzz 작성자가 아닌 수신자는 해당 댓글을 더 이상 받지 못함 — **의도된 변경**
- `BuzzComment`의 `isPublic()` getter는 Lombok `@Getter`로 이미 생성됨 — 별도 추가 불필요
- 사전 검증 메서드(`validateCommentCreation`, `validateCommentUpdate`)와 기존 도메인 메서드(`createComment`, `updateComment`)의 검증 로직이 중복되나, upload guard 목적이 분리되어 있으므로 허용
