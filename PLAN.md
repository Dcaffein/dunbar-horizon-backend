# PLAN: task-45 — Flag 도메인 취약점 수정

## Branch
`ai/fix-flag-domain-issues`

## 작업 목표

Flag 도메인의 접근 권한 검증 누락, 입력 검증 누락, bare orElseThrow를 수정한다.

---

## 변경 파일 목록

| 파일 | 변경 내용 |
|------|-----------|
| `flag/domain/flag/Flag.java` | `validateAccess`, `validateCommentCreation` 도메인 메서드 추가 |
| `flag/application/service/flag/FlagQueryService.java` | `getFlagDetail`에 `flag.validateAccess` 호출 추가 |
| `flag/application/service/comment/FlagCommentQueryService.java` | `FlagParticipantRepository` 의존성 추가, `getCommentTree`에 `flag.validateAccess` 호출 추가 |
| `flag/application/service/comment/FlagCommentCommandService.java` | `FlagParticipantRepository` 의존성 추가, `createRootComment`/`createReply`에 `validateCommentCreation` 호출, `deleteComment` bare orElseThrow 수정 |
| `flag/adapter/in/web/FlagCommentController.java` | `createRootComment`, `createReply`, `updateComment`에 `@Valid` 추가 |

---

## 구현 방향

### Flag.java — 도메인 메서드 추가

```java
public void validateAccess(Long viewerId, Set<Long> participantIds) {
    if (!hostId.equals(viewerId) && !participantIds.contains(viewerId))
        throw new FlagAuthorizationException("플래그에 접근 권한이 없습니다.");
}

public void validateCommentCreation(Long commenterId, Set<Long> participantIds) {
    if (!hostId.equals(commenterId) && !participantIds.contains(commenterId))
        throw new FlagAuthorizationException("플래그 참여자만 댓글을 작성할 수 있습니다.");
}
```

### FlagQueryService.getFlagDetail

이미 participants를 로드하므로 `flag.validateAccess(viewerId, participantIds)` 한 줄 추가.

### FlagCommentQueryService.getCommentTree

`FlagParticipantRepository` 주입 후 participants 로드, `flag.validateAccess(viewerId, participantIds)` 호출.

### FlagCommentCommandService

- `createRootComment`: `existsById` → `findById` + participants 로드 + `flag.validateCommentCreation(userId, participantIds)`
- `createReply`: parent에서 flagId 추출 → flag + participants 로드 + `flag.validateCommentCreation(userId, participantIds)`
- `deleteComment`: `orElseThrow()` → `orElseThrow(() -> new FlagNotFoundException(comment.getFlagId()))`

### FlagCommentController

`@RequestBody` 앞에 `@Valid` 추가 (3곳).
